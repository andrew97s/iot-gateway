package com.zhian.gateway.third;

import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.plugin.PluginCatalog;
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
        ZaSysPlatform pc = new ZaSysPlatform();
        pc.setStatus("1");
        List<ZaSysPlatform> list = zaSysPlatformService.selectZaSysPlatformList(pc);
        for (ZaSysPlatform platform : list) {
            try {
                if (start(platform, START_REASON_AUTO)) {
                    log.info("{} 插件启动成功", platform.getName());
                    platform.setRunning(ZaSysPlatform.STATE_RUNNING);
                } else {
                    log.warn("{} 插件启动失败", platform.getName());
                    platform.setRunning(ZaSysPlatform.STATE_STOP);
                }
                zaSysPlatformService.updateZaSysPlatform(platform);
            } catch (Exception e) {
                log.error("{} 插件启动异常", platform.getName(), e);
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
            stats.put("alive", handler.isAlive());
            stats.put("protocol", handler.getProtocol());
            stats.put("description", handler.getDescription());
            stats.put("connectionInfo", handler.getConnectionInfo());
            stats.put("configSchema", handler.getConfigSchema());
        }
        stats.put("lastStartTime", lastStartTimeMap.get(platformCode));
        stats.put("lastStopTime",  lastStopTimeMap.get(platformCode));
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

        if (!handlerMap.containsKey(zaSysPlatform.getCode())) {
            SpringUtils.getBean(IZaSysErrorService.class).logWithPlatform(
                    zaSysPlatform.getCode(), ZaSysError.TYPE_SERVER,
                    "不支持平台" + zaSysPlatform.getName() + "的接入", null, null);
            String content = buildStartLogContent(reasonLabel, false, "未注册对应处理器");
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_START,
                    zaSysPlatform.getName() + " 启动", content);
            return false;
        }
        ThirdHandler handler = handlerMap.get(zaSysPlatform.getCode());
        boolean ret = handler.start(zaSysPlatform);
        String failMsg = ret ? null : "handler.start 返回 false";
        String content = buildStartLogContent(reasonLabel, ret, failMsg);
        logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_START,
                zaSysPlatform.getName() + " 启动", content);
        if (ret) {
            lastStartTimeMap.put(zaSysPlatform.getCode(), new Date());
            if (ZaSysPlatform.STATE_STOP.equalsIgnoreCase(zaSysPlatform.getRunning())) {
                zaSysPlatform.setRunning(ZaSysPlatform.STATE_RUNNING);
                SpringUtils.getBean(IZaSysPlatformService.class).updateZaSysPlatform(zaSysPlatform);
            }
        }
        return ret;
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
        if (!handlerMap.containsKey(zaSysPlatform.getCode())) {
            SpringUtils.getBean(IZaSysErrorService.class).logWithPlatform(
                    zaSysPlatform.getCode(), ZaSysError.TYPE_SERVER,
                    "不支持平台" + zaSysPlatform.getName() + "的接入", null, null);
            return false;
        }
        ThirdHandler handler = handlerMap.get(zaSysPlatform.getCode());
        boolean ret = handler.stop();
        if (ret) {
            lastStopTimeMap.put(zaSysPlatform.getCode(), new Date());
            if (ZaSysPlatform.STATE_RUNNING.equalsIgnoreCase(zaSysPlatform.getRunning())) {
                zaSysPlatform.setRunning(ZaSysPlatform.STATE_STOP);
                SpringUtils.getBean(IZaSysPlatformService.class).updateZaSysPlatform(zaSysPlatform);
            }
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_STOP,
                    zaSysPlatform.getName() + " 已停止", null);
        } else {
            logService.recordByCode(zaSysPlatform.getCode(), ZaPlatformLog.TYPE_ERROR,
                    zaSysPlatform.getName() + " 停止失败", "handler.stop 返回 false");
        }
        return ret;
    }
}
