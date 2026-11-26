package com.zhian.gateway.third;

import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.plugin.PluginCatalog;
import com.zhian.gateway.plugin.runtime.PluginHealthMonitor;
import com.zhian.gateway.plugin.runtime.PluginRuntimeRegistry;
import com.zhian.gateway.plugin.runtime.PluginRuntimeSnapshot;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 启动各平台对接插件，并维护运行时统计信息
 */
@Slf4j
@Component
public class ThirdApplicationRunner implements ApplicationRunner {

    /** 应用启动时批量拉起插件 */
    public static final String START_REASON_AUTO = "自动启动";
    /** 接口 / 管理端启停、保存配置触发的启停 */
    public static final String START_REASON_MANUAL = "手动启动";

    private static Map<String, ThirdHandler> handlerMap = new ConcurrentHashMap<>();

    /** 各平台最近一次启动时间 */
    private static Map<String, Date> lastStartTimeMap = new ConcurrentHashMap<>();
    /** 各平台最近一次停止时间 */
    private static Map<String, Date> lastStopTimeMap  = new ConcurrentHashMap<>();
    /** 各平台累计处理消息数 */
    private static Map<String, AtomicLong> msgCountMap = new ConcurrentHashMap<>();
    /** 各平台累计错误数 */
    private static Map<String, AtomicLong> errCountMap = new ConcurrentHashMap<>();

    @Autowired
    private IZaSysPlatformService zaSysPlatformService;
    @Autowired
    private IZaSysErrorService zaSysErrorService;
    @Autowired
    private IZaPlatformLogService zaPlatformLogService;
    @Autowired(required = false)
    private PluginCatalog pluginCatalog;
    @Autowired
    private PluginRuntimeRegistry runtimeRegistry;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (ThirdHandler handler : SpringUtils.getBeanList(ThirdHandler.class)) {
            handlerMap.put(handler.getPlatform(), handler);
        }
        if (pluginCatalog != null) {
            try {
                pluginCatalog.refresh();
            } catch (Exception e) {
                log.warn("刷新插件目录失败: {}", e.getMessage());
            }
        }
        // 独立线程，不阻塞 Spring 启动
        new Thread(this::startAll).start();
    }

    private void startAll() {
        List<ZaSysPlatform> list = zaSysPlatformService.selectZaSysPlatformList(new ZaSysPlatform());
        for (ZaSysPlatform platform : list) {
            if (!"1".equals(platform.getStatus())) {
                runtimeRegistry.markStopped(platform.getCode(), "实例未启用");
                continue;
            }
            try {
                if (start(platform, START_REASON_AUTO)) {
                    log.info("{} 插件启动成功", platform.getName());
                } else {
                    log.warn("{} 插件启动失败", platform.getName());
                }
            } catch (Exception e) {
                log.error("{} 插件启动异常", platform.getName(), e);
                runtimeRegistry.markAbnormal(platform.getCode(), "启动异常: " + e.getMessage());
                zaSysErrorService.logWithPlatform(platform.getCode(), ZaSysError.TYPE_SERVER,
                        platform.getName() + "启动失败", e.getMessage(), platform.getConfig());
                zaPlatformLogService.recordByCode(platform.getCode(), ZaPlatformLog.TYPE_START,
                        platform.getName() + " 启动",
                        buildStartLogContent(START_REASON_AUTO, false, e.getMessage()));
            }
        }
    }

    /** 获取服务处理接口 */
    public static ThirdHandler getHandler(String platformCode) {
        return handlerMap.get(platformCode);
    }

    /** 获取所有已注册的平台代码 */
    public static Set<String> getRegisteredPlatforms() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }

    /**
     * 获取指定平台的运行时统计信息
     */
    public static Map<String, Object> getPlatformStats(String platformCode) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("platformCode", platformCode);
        stats.put("registered", handlerMap.containsKey(platformCode));
        ThirdHandler handler = handlerMap.get(platformCode);
        if (handler != null) {
            stats.put("protocol", handler.getProtocol());
            stats.put("description", handler.getDescription());
            stats.put("connectionInfo", handler.getConnectionInfo());
            stats.put("configSchema", handler.getConfigSchema());
        }
        stats.put("lastStartTime", lastStartTimeMap.get(platformCode));
        stats.put("lastStopTime",  lastStopTimeMap.get(platformCode));
        PluginRuntimeSnapshot runtime = runtimeRegistry().get(platformCode);
        if (runtime != null) {
            stats.put("alive", runtime.getState() == com.zhian.gateway.plugin.runtime.PluginState.RUNNING);
            stats.put("state", runtime.getState().getCode());
            stats.put("healthReason", runtime.getHealthReason());
            stats.put("lastHealthCheckTime", runtime.getLastHealthCheckTime());
            stats.put("lastStateChangeTime", runtime.getLastStateChangeTime());
            stats.put("consecutiveFailures", runtime.getConsecutiveFailures());
            stats.put("consecutiveSuccesses", runtime.getConsecutiveSuccesses());
            stats.put("restartCount", runtime.getRestartCount());
            stats.put("nextRestartTime", runtime.getNextRestartTime());
        } else {
            stats.put("alive", false);
        }
        AtomicLong msgCount = msgCountMap.get(platformCode);
        stats.put("msgCount", msgCount == null ? 0L : msgCount.get());
        AtomicLong errCount = errCountMap.get(platformCode);
        stats.put("errCount", errCount == null ? 0L : errCount.get());
        return stats;
    }

    /**
     * 获取所有平台的统计信息快照
     */
    public static List<Map<String, Object>> getAllPlatformStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String code : handlerMap.keySet()) {
            result.add(getPlatformStats(code));
        }
        return result;
    }

    /** 外部调用：增加消息计数 */
    public static void incrementMsgCount(String platformCode) {
        msgCountMap.computeIfAbsent(platformCode, k -> new AtomicLong(0)).incrementAndGet();
    }

    /** 外部调用：增加错误计数 */
    public static void incrementErrCount(String platformCode) {
        errCountMap.computeIfAbsent(platformCode, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 启动插件（默认视为手动启动，兼容旧调用）
     */
    public static boolean start(ZaSysPlatform zaSysPlatform) {
        return start(zaSysPlatform, START_REASON_MANUAL);
    }

    /**
     * 启动插件并写入运行日志（原因 + 结果）
     *
     * @param startReason {@link #START_REASON_AUTO} 或 {@link #START_REASON_MANUAL}
     */
    public static boolean start(ZaSysPlatform zaSysPlatform, String startReason) {
        IZaPlatformLogService logService = SpringUtils.getBean(IZaPlatformLogService.class);
        String reasonLabel = StringUtils.isEmpty(startReason) ? START_REASON_MANUAL : startReason;
        PluginRuntimeRegistry registry = runtimeRegistry();
        ReentrantLock lock = registry.lifecycleLock(zaSysPlatform.getCode());
        lock.lock();
        try {
            registry.markStarting(zaSysPlatform.getCode(), reasonLabel + "中");

            if (!handlerMap.containsKey(zaSysPlatform.getCode())) {
                String failMsg = "未注册对应处理器";
                SpringUtils.getBean(IZaSysErrorService.class).logWithPlatform(
                        zaSysPlatform.getCode(), ZaSysError.TYPE_SERVER,
                        "不支持平台" + zaSysPlatform.getName() + "的接入", null, null);
                logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_START,
                        zaSysPlatform.getName() + " 启动",
                        buildStartLogContent(reasonLabel, false, failMsg));
                registry.markAbnormal(zaSysPlatform.getCode(), failMsg);
                return false;
            }
            ThirdHandler handler = handlerMap.get(zaSysPlatform.getCode());
            boolean ret = handler.start(zaSysPlatform);
            String failMsg = ret ? null : "handler.start 返回 false";
            if (ret) {
                PluginHealthResult health = SpringUtils.getBean(PluginHealthMonitor.class)
                        .probeNow(zaSysPlatform.getCode());
                ret = health.isHealthy();
                failMsg = ret ? null : health.getMessage();
            }
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_START,
                    zaSysPlatform.getName() + " 启动",
                    buildStartLogContent(reasonLabel, ret, failMsg));
            if (ret) {
                lastStartTimeMap.put(zaSysPlatform.getCode(), new Date());
                registry.markRunning(zaSysPlatform.getCode(), "启动成功");
            } else {
                registry.markAbnormal(zaSysPlatform.getCode(), failMsg);
            }
            return ret;
        } catch (Exception e) {
            registry.markAbnormal(zaSysPlatform.getCode(), "启动异常: " + e.getMessage());
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_START,
                    zaSysPlatform.getName() + " 启动",
                    buildStartLogContent(reasonLabel, false, e.getMessage()));
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static String buildStartLogContent(String reasonLabel, boolean success, String failDetail) {
        StringBuilder sb = new StringBuilder();
        sb.append("启动原因：").append(reasonLabel).append("\n");
        sb.append("启动结果：").append(success ? "成功" : "失败");
        if (!success && StringUtils.isNotEmpty(failDetail)) {
            sb.append("\n失败说明：").append(failDetail);
        }
        return sb.toString();
    }

    /** 停止 */
    public static boolean stop(ZaSysPlatform zaSysPlatform) {
        IZaPlatformLogService logService = SpringUtils.getBean(IZaPlatformLogService.class);
        PluginRuntimeRegistry registry = runtimeRegistry();
        ReentrantLock lock = registry.lifecycleLock(zaSysPlatform.getCode());
        lock.lock();
        try {
            if (!handlerMap.containsKey(zaSysPlatform.getCode())) {
                registry.markStopped(zaSysPlatform.getCode(), "实例已停止（处理器未注册）");
                return true;
            }
            ThirdHandler handler = handlerMap.get(zaSysPlatform.getCode());
            boolean ret = handler.stop();
            if (ret) {
                lastStopTimeMap.put(zaSysPlatform.getCode(), new Date());
                registry.markStopped(zaSysPlatform.getCode(), "手动停止");
                logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_STOP,
                        zaSysPlatform.getName() + " 已停止", null);
            } else {
                registry.markAbnormal(zaSysPlatform.getCode(), "handler.stop 返回 false");
                logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_ERROR,
                        zaSysPlatform.getName() + " 停止失败", "handler.stop 返回 false");
            }
            return ret;
        } catch (Exception e) {
            registry.markAbnormal(zaSysPlatform.getCode(), "停止异常: " + e.getMessage());
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_ERROR,
                    zaSysPlatform.getName() + " 停止异常", e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 健康巡检触发的自动重启。状态保持异常，直到连续健康检查达到恢复阈值。
     */
    public static boolean restartForHealth(ZaSysPlatform platform) {
        PluginRuntimeRegistry registry = runtimeRegistry();
        ReentrantLock lock = registry.lifecycleLock(platform.getCode());
        lock.lock();
        try {
            ThirdHandler handler = handlerMap.get(platform.getCode());
            if (handler == null) {
                registry.markAbnormal(platform.getCode(), "未注册对应处理器");
                return false;
            }
            try {
                handler.stop();
            } catch (Exception e) {
                log.warn("{} 自动重启前停止异常: {}", platform.getName(), e.getMessage());
            }
            boolean started = handler.start(platform);
            if (started) {
                lastStartTimeMap.put(platform.getCode(), new Date());
                registry.markRestarted(platform.getCode());
            } else {
                registry.markAbnormal(platform.getCode(), "自动重启失败");
            }
            return started;
        } catch (Exception e) {
            registry.markAbnormal(platform.getCode(), "自动重启异常: " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static PluginRuntimeRegistry runtimeRegistry() {
        return SpringUtils.getBean(PluginRuntimeRegistry.class);
    }
}
