package com.zhian.gateway.plugin.runtime;

import com.zhian.gateway.plugin.PluginCatalog;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.system.service.ISysConfigService;
import com.zhian.gateway.third.PluginHealthResult;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 插件健康巡检与自动恢复。
 */
@Slf4j
@Component
public class PluginHealthMonitor {

    public static final String KEY_AUTO_RESTART = "gateway.plugin.autorestart";
    public static final String KEY_INTERVAL = "gateway.plugin.health.interval.seconds";
    public static final String KEY_TIMEOUT = "gateway.plugin.health.timeout.seconds";
    public static final String KEY_FAILURE_THRESHOLD = "gateway.plugin.health.failure.threshold";
    public static final String KEY_RECOVERY_THRESHOLD = "gateway.plugin.health.recovery.threshold";
    public static final String KEY_MAX_RESTARTS = "gateway.plugin.restart.maxAttempts";

    private final ExecutorService probeExecutor = new ThreadPoolExecutor(
            0,
            8,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread thread = new Thread(r, "plugin-health-probe");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    @Autowired
    private IZaSysPlatformService platformService;
    @Autowired
    private IZaPlatformLogService platformLogService;
    @Autowired
    private IZaSysErrorService errorService;
    @Autowired
    private ISysConfigService configService;
    @Autowired
    private PluginRuntimeRegistry runtimeRegistry;
    @Autowired
    private PluginCatalog pluginCatalog;

    private volatile long nextSweepAt;

    @Scheduled(fixedDelay = 1000L)
    public void sweep() {
        int intervalSeconds = configInt(KEY_INTERVAL, 10, 2, 3600);
        long now = System.currentTimeMillis();
        if (now < nextSweepAt) {
            return;
        }
        nextSweepAt = now + intervalSeconds * 1000L;

        ZaSysPlatform query = new ZaSysPlatform();
        query.setStatus("1");
        List<ZaSysPlatform> enabled = platformService.selectZaSysPlatformList(query);
        for (ZaSysPlatform platform : enabled) {
            if (pluginCatalog.isExcluded(platform.getCode())) {
                continue;
            }
            inspect(platform);
        }
    }

    private void inspect(ZaSysPlatform platform) {
        String code = platform.getCode();
        ReentrantLock lock = runtimeRegistry.lifecycleLock(code);
        if (!lock.tryLock()) {
            return;
        }
        try {
            doInspect(platform);
        } finally {
            lock.unlock();
        }
    }

    private void doInspect(ZaSysPlatform platform) {
        String code = platform.getCode();
        PluginRuntimeSnapshot before = runtimeRegistry.get(code);
        if (before == null || before.getState() == PluginState.STOPPED) {
            return;
        }
        PluginHealthResult health = probeNow(code);
        PluginRuntimeSnapshot after = runtimeRegistry.recordHealth(
                code,
                health,
                configInt(KEY_FAILURE_THRESHOLD, 3, 1, 20),
                configInt(KEY_RECOVERY_THRESHOLD, 2, 1, 20)
        );

        if (before != null && before.getState() != after.getState()) {
            platformLogService.recordByCode(code,
                    after.getState() == PluginState.RUNNING ? ZaPlatformLog.TYPE_START : ZaPlatformLog.TYPE_ERROR,
                    platform.getName() + " 状态变更为 " + after.getState().getCode(),
                    health.getMessage());
        }

        if (after.getState() != PluginState.ABNORMAL
                || !"true".equalsIgnoreCase(config(KEY_AUTO_RESTART, "true"))) {
            return;
        }

        int maxRestarts = platform.getConfigInt("maxRestart") == null
                ? configInt(KEY_MAX_RESTARTS, 3, 0, 20)
                : Math.max(0, platform.getConfigInt("maxRestart"));
        if (!runtimeRegistry.prepareAutoRestart(code, maxRestarts, 10_000L)) {
            return;
        }

        boolean restarted = ThirdApplicationRunner.restartForHealth(platform);
        String message = restarted ? "自动重启已执行，等待健康确认" : "自动重启失败";
        platformLogService.recordByCode(code,
                restarted ? ZaPlatformLog.TYPE_START : ZaPlatformLog.TYPE_ERROR,
                platform.getName() + " 自动重启", message);
        if (!restarted) {
            errorService.logWithPlatform(code, ZaSysError.TYPE_API_TIMEOUT,
                    platform.getName() + "自动重启失败", health.getMessage(), null);
        }
    }

    public PluginHealthResult probeNow(String code) {
        ThirdHandler handler = ThirdApplicationRunner.getHandler(code);
        if (handler == null) {
            return PluginHealthResult.unhealthy("未注册对应处理器");
        }
        int timeoutSeconds = configInt(KEY_TIMEOUT, 3, 1, 60);
        Future<PluginHealthResult> future;
        try {
            future = probeExecutor.submit(handler::checkHealth);
        } catch (RejectedExecutionException e) {
            return PluginHealthResult.unhealthy("健康检查线程已耗尽，可能存在卡死的插件检查");
        }
        try {
            PluginHealthResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return result == null ? PluginHealthResult.unhealthy("健康检查未返回结果") : result;
        } catch (TimeoutException e) {
            future.cancel(true);
            return PluginHealthResult.unhealthy("健康检查超时（" + timeoutSeconds + "秒）");
        } catch (Exception e) {
            return PluginHealthResult.unhealthy("健康检查异常: " + rootMessage(e));
        }
    }

    private int configInt(String key, int defaultValue, int min, int max) {
        try {
            int value = Integer.parseInt(configService.selectConfigByKey(key));
            return Math.max(min, Math.min(max, value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String config(String key, String defaultValue) {
        try {
            String value = configService.selectConfigByKey(key);
            return value == null || value.trim().isEmpty() ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @PreDestroy
    public void shutdown() {
        probeExecutor.shutdownNow();
    }
}
