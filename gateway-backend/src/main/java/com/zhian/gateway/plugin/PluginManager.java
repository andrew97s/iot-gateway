package com.zhian.gateway.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.plugin.runtime.PluginRuntimeRegistry;
import com.zhian.gateway.plugin.runtime.PluginRuntimeSnapshot;
import com.zhian.gateway.plugin.runtime.PluginState;
import com.zhian.gateway.plugin.runtime.PluginHealthMonitor;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.PluginHealthResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 接入插件管理器：类型目录 + 实例生命周期（安装/配置/启停/卸载）
 */
@Slf4j
@Service
public class PluginManager {

    @Autowired
    private PluginCatalog catalog;
    @Autowired
    private IZaSysPlatformService platformService;
    @Autowired
    private IZaPlatformLogService platformLogService;
    @Autowired
    private IZaSysDeviceService deviceService;
    @Autowired
    private IZaSysMessageService messageService;
    @Autowired
    private PluginRuntimeRegistry runtimeRegistry;
    @Autowired
    private PluginHealthMonitor healthMonitor;

    public List<PluginDescriptor> listTypes() {
        catalog.refresh();
        return catalog.listTypes();
    }

    public PluginDescriptor getType(String pluginId) {
        catalog.refresh();
        PluginDescriptor d = catalog.getType(pluginId);
        if (d == null) {
            throw new ServiceException("插件类型不存在: " + pluginId);
        }
        return d;
    }

    public List<PluginInstanceView> listInstances(String keyword, String state, String protocol) {
        List<ZaSysPlatform> all = platformService.selectZaSysPlatformList(new ZaSysPlatform());
        Map<String, Map<String, Object>> todayMap = indexByPf(messageService.countTodayByPlatform());
        Map<String, Map<String, Object>> deviceMap = indexDeviceStats();
        List<PluginInstanceView> views = all.stream()
                .filter(p -> !catalog.isExcluded(resolvePluginId(p)))
                .map(p -> toView(p, todayMap, deviceMap))
                .collect(Collectors.toList());
        Set<String> instantiatedTypes = all.stream()
                .map(PluginManager::resolvePluginId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (PluginDescriptor descriptor : catalog.listTypes()) {
            if (!instantiatedTypes.contains(descriptor.getId())) {
                views.add(toInstalledView(descriptor));
            }
        }
        return views.stream()
                .filter(v -> matchKeyword(v, keyword))
                .filter(v -> StringUtils.isEmpty(state) || state.equals(v.getState()))
                .filter(v -> StringUtils.isEmpty(protocol) || protocol.equalsIgnoreCase(v.getProtocol()))
                .sorted(Comparator.comparing(PluginInstanceView::getName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    public PluginInstanceView getInstance(String instanceId) {
        ZaSysPlatform p = requireInstance(instanceId);
        return toView(p, indexByPf(messageService.countTodayByPlatform()), indexDeviceStats());
    }

    /**
     * 从已有插件类型安装实例（当前架构：instanceId = pluginId，1:1）
     */
    public PluginInstanceView install(String pluginId, String instanceId, String name, String ip, Integer port) {
        catalog.refresh();
        PluginDescriptor type = catalog.getType(pluginId);
        if (type == null) {
            throw new ServiceException("未知插件类型: " + pluginId);
        }
        if (!type.isRunnable()) {
            throw new ServiceException("插件类型 " + pluginId + " 尚未内置可运行模块，无法创建实例");
        }
        String code = StringUtils.isNotEmpty(instanceId) ? instanceId.trim() : pluginId;
        // 现有 Handler 以 getPlatform() 为唯一键，实例编码必须与类型一致
        if (!pluginId.equals(code)) {
            throw new ServiceException("当前运行时要求实例编码与插件类型一致（" + pluginId + "），暂不支持多实例");
        }
        if (platformService.selectZaSysPlatformByCode(code) != null) {
            throw new ServiceException("插件实例已存在: " + code);
        }

        JSONObject config = new JSONObject();
        config.put("_pluginType", pluginId);
        config.put("_vendor", type.getVendor());
        config.put("_version", type.getVersion());
        config.put("_capabilities", type.getCapabilities());
        config.put("protocol", type.getProtocol());
        applySchemaDefaults(type.getConfigSchema(), config);

        ZaSysPlatform platform = new ZaSysPlatform();
        platform.setCode(code);
        platform.setName(StringUtils.isNotEmpty(name) ? name : type.getName());
        platform.setIp(ip);
        platform.setPort(port);
        platform.setStatus("0");
        platform.setConfig(config.toJSONString());
        platform.setRemark("由插件管理安装");
        platformService.insertZaSysPlatform(platform);

        platformLogService.recordByCode(code, ZaPlatformLog.TYPE_OTHER,
                platform.getName() + " 已安装",
                "pluginId=" + pluginId + ", version=" + type.getVersion());
        runtimeRegistry.markStopped(code, "实例已创建，尚未启用");
        return getInstance(code);
    }

    public PluginInstanceView updateConfig(String instanceId, Map<String, Object> body) {
        ZaSysPlatform platform = requireInstance(instanceId);
        PluginDescriptor type = catalog.getType(resolvePluginId(platform));

        JSONObject config = platform.getConfigObject() != null
                ? new JSONObject(platform.getConfigObject())
                : new JSONObject();
        if (body != null) {
            if (body.get("ip") != null) {
                platform.setIp(String.valueOf(body.get("ip")));
            }
            if (body.containsKey("port")) {
                Object port = body.get("port");
                platform.setPort(port == null || "".equals(port) ? null : Integer.valueOf(String.valueOf(port)));
            }
            if (body.get("apis") != null) {
                platform.setApis(String.valueOf(body.get("apis")));
            }
            if (body.get("remark") != null) {
                platform.setRemark(String.valueOf(body.get("remark")));
            }
            if (body.get("name") != null && StringUtils.isNotEmpty(String.valueOf(body.get("name")))) {
                platform.setName(String.valueOf(body.get("name")));
            }
            Object cfg = body.get("config");
            if (cfg instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) cfg;
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (e.getKey() != null && !e.getKey().startsWith("_")) {
                        config.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        if (type != null) {
            config.put("_pluginType", type.getId());
            config.put("_vendor", type.getVendor());
            config.put("_version", type.getVersion());
            config.put("_capabilities", type.getCapabilities());
            validateRequired(type.getConfigSchema(), config, platform);
        }
        platform.setConfig(config.toJSONString());

        // 热生效：若已启用则重启
        boolean wasEnabled = "1".equals(platform.getStatus());
        platformService.updateZaSysPlatform(platform);
        platformLogService.recordByPlatformId(platform.getId(), ZaPlatformLog.TYPE_CONFIG,
                platform.getName() + " 配置已更新",
                JSON.toJSONString(config));

        if (wasEnabled) {
            restartRuntime(platform);
        }
        return getInstance(instanceId);
    }

    public PluginInstanceView start(String instanceId) {
        ZaSysPlatform platform = requireInstance(instanceId);
        ensureRunnable(platform);
        platform.setStatus("1");
        platformService.updateZaSysPlatform(platform);
        boolean ok = ThirdApplicationRunner.start(platform, ThirdApplicationRunner.START_REASON_MANUAL);
        if (!ok) {
            throw new ServiceException("插件启动失败，请查看运行日志");
        }
        return getInstance(instanceId);
    }

    public PluginInstanceView stop(String instanceId) {
        ZaSysPlatform platform = requireInstance(instanceId);
        platform.setStatus("0");
        platformService.updateZaSysPlatform(platform);
        if (!ThirdApplicationRunner.stop(platform)) {
            throw new ServiceException("插件停止失败，请查看运行日志");
        }
        return getInstance(instanceId);
    }

    public PluginInstanceView restart(String instanceId) {
        ZaSysPlatform platform = requireInstance(instanceId);
        ensureRunnable(platform);
        platform.setStatus("1");
        platformService.updateZaSysPlatform(platform);
        restartRuntime(platform);
        return getInstance(instanceId);
    }

    public void uninstall(String instanceId) {
        ZaSysPlatform platform = requireInstance(instanceId);
        if ("1".equals(platform.getStatus())) {
            try {
                ThirdApplicationRunner.stop(platform);
            } catch (Exception ignored) {
            }
        }
        // 先写日志再删实例
        platformLogService.recordByPlatformId(platform.getId(), ZaPlatformLog.TYPE_OTHER,
                platform.getName() + " 已卸载", "instanceId=" + instanceId);
        platformService.deleteZaSysPlatformById(platform.getId());
        runtimeRegistry.remove(instanceId);
    }

    public Map<String, Object> testConnection(String instanceId) {
        ZaSysPlatform platform = requireInstance(instanceId);
        Map<String, Object> result = new LinkedHashMap<>();
        ThirdHandler handler = ThirdApplicationRunner.getHandler(resolvePluginId(platform));
        result.put("instanceId", instanceId);
        result.put("registered", handler != null);
        if (handler == null) {
            result.put("success", false);
            result.put("message", "未注册对应处理器");
            return result;
        }
        PluginHealthResult health;
        if (!"1".equals(platform.getStatus())) {
            ReentrantLock lock = runtimeRegistry.lifecycleLock(instanceId);
            lock.lock();
            try {
                boolean started = handler.start(platform);
                health = started
                        ? healthMonitor.probeNow(resolvePluginId(platform))
                        : PluginHealthResult.unhealthy("临时启动失败");
                handler.stop();
                result.put("probed", true);
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "连接探测失败: " + e.getMessage());
                result.put("probed", true);
                return result;
            } finally {
                lock.unlock();
            }
        } else {
            health = healthMonitor.probeNow(resolvePluginId(platform));
        }
        result.put("success", health.isHealthy());
        result.put("alive", health.isHealthy());
        result.put("connectionInfo", handler.getConnectionInfo());
        result.put("message", health.getMessage());
        return result;
    }

    public Map<String, Object> summary() {
        List<PluginInstanceView> list = listInstances(null, null, null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", list.size());
        map.put("running", list.stream().filter(v -> "running".equals(v.getState())).count());
        map.put("stopped", list.stream().filter(v -> "stopped".equals(v.getState())).count());
        map.put("installed", list.stream().filter(v -> "installed".equals(v.getState())).count());
        map.put("abnormal", list.stream().filter(v -> "abnormal".equals(v.getState())).count());
        map.put("enabled", list.stream().filter(v -> "running".equals(v.getState()) || "abnormal".equals(v.getState())).count());
        return map;
    }

    public int getRestartCount(String instanceId) {
        PluginRuntimeSnapshot snapshot = runtimeRegistry.get(instanceId);
        return snapshot == null ? 0 : snapshot.getRestartCount();
    }

    // ---------- helpers ----------

    private ZaSysPlatform requireInstance(String instanceId) {
        ZaSysPlatform p = platformService.selectZaSysPlatformByCode(instanceId);
        if (p == null) {
            throw new ServiceException("插件实例不存在: " + instanceId);
        }
        if (catalog.isExcluded(resolvePluginId(p))) {
            throw new ServiceException("该平台不是接入插件实例");
        }
        return p;
    }

    private void ensureRunnable(ZaSysPlatform platform) {
        String type = resolvePluginId(platform);
        if (ThirdApplicationRunner.getHandler(type) == null) {
            throw new ServiceException("插件类型未注册可运行模块: " + type);
        }
    }

    public static String resolvePluginId(ZaSysPlatform platform) {
        if (platform == null) {
            return null;
        }
        if (platform.getConfigObject() != null) {
            String t = platform.getConfigObject().getString("_pluginType");
            if (StringUtils.isNotEmpty(t)) {
                return t;
            }
        }
        return platform.getCode();
    }

    private PluginInstanceView toView(ZaSysPlatform p,
                                      Map<String, Map<String, Object>> todayMap,
                                      Map<String, Map<String, Object>> deviceMap) {
        String pluginId = resolvePluginId(p);
        PluginDescriptor type = catalog.getType(pluginId);
        Map<String, Object> runtime = ThirdApplicationRunner.getPlatformStats(pluginId);
        // 统计按实例 code 与类型 code 对齐（当前 1:1）
        Map<String, Object> today = todayMap.getOrDefault(p.getCode(), Collections.emptyMap());
        Map<String, Object> device = deviceMap.getOrDefault(p.getCode(), Collections.emptyMap());

        PluginInstanceView v = new PluginInstanceView();
        v.setId(p.getId());
        v.setInstanceId(p.getCode());
        v.setPluginId(pluginId);
        v.setName(p.getName());
        v.setIp(p.getIp());
        v.setPort(p.getPort());
        v.setApis(p.getApis());
        v.setRemark(p.getRemark());
        v.setStatus(p.getStatus());
        v.setConfig(p.getConfigObject() != null ? new LinkedHashMap<>(p.getConfigObject()) : new LinkedHashMap<>());

        if (type != null) {
            v.setVendor(type.getVendor());
            v.setProtocol(type.getProtocol());
            v.setVersion(type.getVersion());
            v.setCapabilities(type.getCapabilities());
            v.setConfigSchema(type.getConfigSchema());
        } else {
            v.setProtocol(String.valueOf(runtime.getOrDefault("protocol", pluginId)));
            v.setVersion(metaFromConfig(p, "_version", "—"));
            v.setVendor(metaFromConfig(p, "_vendor", ""));
            v.setCapabilities(capsFromConfig(p));
            Object schema = runtime.get("configSchema");
            if (schema instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> s = (List<Map<String, Object>>) schema;
                v.setConfigSchema(s);
            }
        }
        if (StringUtils.isEmpty(v.getProtocol()) && runtime.get("protocol") != null) {
            v.setProtocol(String.valueOf(runtime.get("protocol")));
        }

        PluginRuntimeSnapshot snapshot = runtimeRegistry.get(p.getCode());
        PluginState state;
        if (snapshot != null) {
            state = snapshot.getState();
        } else {
            state = "1".equals(p.getStatus()) ? PluginState.ABNORMAL : PluginState.STOPPED;
        }
        v.setAlive(state == PluginState.RUNNING);
        v.setConnectionInfo(runtime.get("connectionInfo") != null ? String.valueOf(runtime.get("connectionInfo")) : null);
        v.setMsgCount(toLong(runtime.get("msgCount")));
        v.setErrCount(toLong(runtime.get("errCount")));
        v.setLastStartTime((Date) runtime.get("lastStartTime"));
        v.setLastStopTime((Date) runtime.get("lastStopTime"));
        v.setDeviceCount(toLong(device.get("total")));
        v.setTodayMsgCount(toLong(today.get("total")));
        v.setTodayFailCount(toLong(today.get("failedCount")));
        applyRuntime(v, snapshot, state);
        return v;
    }

    private PluginInstanceView toInstalledView(PluginDescriptor type) {
        PluginInstanceView view = new PluginInstanceView();
        view.setPluginId(type.getId());
        view.setName(type.getName());
        view.setVendor(type.getVendor());
        view.setProtocol(type.getProtocol());
        view.setVersion(type.getVersion());
        view.setCapabilities(type.getCapabilities());
        view.setConfigSchema(type.getConfigSchema());
        view.setState(PluginState.INSTALLED.getCode());
        view.setStatus("0");
        view.setAlive(false);
        view.setHealthReason(type.isRunnable() ? "插件类型已安装，尚未创建实例" : "插件包已安装，但未注册可运行处理器");
        view.setConfig(new LinkedHashMap<>());
        return view;
    }

    private void applyRuntime(PluginInstanceView view, PluginRuntimeSnapshot snapshot, PluginState fallback) {
        view.setState((snapshot == null ? fallback : snapshot.getState()).getCode());
        if (snapshot == null) {
            view.setHealthReason(fallback == PluginState.STOPPED ? "实例未启用" : "等待运行时初始化");
            view.setRestartCount(0);
            view.setConsecutiveFailures(0);
            view.setConsecutiveSuccesses(0);
            return;
        }
        view.setHealthReason(snapshot.getHealthReason());
        view.setLastHealthCheckTime(snapshot.getLastHealthCheckTime());
        view.setLastStateChangeTime(snapshot.getLastStateChangeTime());
        view.setNextRestartTime(snapshot.getNextRestartTime());
        view.setRestartCount(snapshot.getRestartCount());
        view.setConsecutiveFailures(snapshot.getConsecutiveFailures());
        view.setConsecutiveSuccesses(snapshot.getConsecutiveSuccesses());
        view.setLastStartTime(snapshot.getLastStartTime());
        view.setLastStopTime(snapshot.getLastStopTime());
    }

    private void restartRuntime(ZaSysPlatform platform) {
        try {
            ThirdApplicationRunner.stop(platform);
            platform.setStatus("1");
            boolean ok = ThirdApplicationRunner.start(platform, ThirdApplicationRunner.START_REASON_MANUAL);
            if (!ok) {
                throw new ServiceException("插件重启失败，请查看运行日志");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("插件重启异常，请查看运行日志").setDetailMessage(e.getMessage());
        }
    }

    private boolean matchKeyword(PluginInstanceView v, String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            return true;
        }
        String kw = keyword.toLowerCase();
        String blob = (v.getName() + " " + v.getVendor() + " " + v.getPluginId() + " " + v.getInstanceId() + " " + v.getProtocol()).toLowerCase();
        return blob.contains(kw);
    }

    private void applySchemaDefaults(List<Map<String, Object>> schema, JSONObject config) {
        if (schema == null) {
            return;
        }
        for (Map<String, Object> field : schema) {
            Object code = field.get("code") != null ? field.get("code") : field.get("key");
            if (code == null) {
                continue;
            }
            String key = String.valueOf(code);
            if (!config.containsKey(key) && field.get("defaultValue") != null) {
                config.put(key, field.get("defaultValue"));
            }
        }
    }

    private void validateRequired(List<Map<String, Object>> schema, JSONObject config, ZaSysPlatform platform) {
        if (schema == null) {
            return;
        }
        for (Map<String, Object> field : schema) {
            if (!Boolean.TRUE.equals(field.get("required"))) {
                continue;
            }
            Object code = field.get("code") != null ? field.get("code") : field.get("key");
            if (code == null) {
                continue;
            }
            String key = String.valueOf(code);
            if ("ip".equals(key)) {
                if (StringUtils.isEmpty(platform.getIp())) {
                    throw new ServiceException("请填写: " + field.getOrDefault("name", key));
                }
                continue;
            }
            if ("port".equals(key)) {
                if (platform.getPort() == null) {
                    throw new ServiceException("请填写: " + field.getOrDefault("name", key));
                }
                continue;
            }
            Object val = config.get(key);
            if (val == null || String.valueOf(val).trim().isEmpty()) {
                throw new ServiceException("请填写: " + field.getOrDefault("name", key));
            }
        }
    }

    private Map<String, Map<String, Object>> indexByPf(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object pf = row.get("pfCode");
                if (pf != null) {
                    map.put(String.valueOf(pf), row);
                }
            }
        }
        return map;
    }

    private Map<String, Map<String, Object>> indexDeviceStats() {
        Map<String, Map<String, Object>> map = new HashMap<>();
        try {
            List<Map<String, Object>> rows = deviceService.selectOnlineStats();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object pf = row.get("pfCode");
                    if (pf != null) {
                        map.put(String.valueOf(pf), row);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("加载设备统计失败: {}", e.getMessage());
        }
        return map;
    }

    private String metaFromConfig(ZaSysPlatform p, String key, String def) {
        if (p.getConfigObject() == null || p.getConfigObject().get(key) == null) {
            return def;
        }
        return String.valueOf(p.getConfigObject().get(key));
    }

    @SuppressWarnings("unchecked")
    private List<String> capsFromConfig(ZaSysPlatform p) {
        if (p.getConfigObject() == null) {
            return Collections.emptyList();
        }
        Object caps = p.getConfigObject().get("_capabilities");
        if (caps instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object c : (List<?>) caps) {
                if (c != null) {
                    list.add(String.valueOf(c));
                }
            }
            return list;
        }
        if (caps instanceof String && StringUtils.isNotEmpty((String) caps)) {
            return Arrays.asList(((String) caps).split("\\s*/\\s*|\\s*,\\s*"));
        }
        return Collections.emptyList();
    }

    private long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }
}
