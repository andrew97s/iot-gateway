package com.zhian.gateway.sys.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaDeviceType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.mapper.ZaAlarmTypeMapper;
import com.zhian.gateway.sys.mapper.ZaDeviceTypeMapper;
import com.zhian.gateway.sys.mapper.ZaMonitorTypeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 标准类型映射服务
 *
 * 将接入插件上报的厂商别名（告警码 / 设备类型码 / 监测项码）解析为
 * 网关标准类型（za_alarm_type / za_device_type / za_monitor_type），
 * 供统一消息转换器使用。
 *
 * 别名索引全量缓存于内存（配置数据量小），类型配置变更后调用 {@link #reload()} 立即生效，
 * 兜底每 60 秒自动刷新一次。
 */
@Slf4j
@Service
public class TypeMappingService {

    private static final long REFRESH_INTERVAL_MS = 60_000L;

    @Autowired
    private ZaAlarmTypeMapper alarmTypeMapper;
    @Autowired
    private ZaDeviceTypeMapper deviceTypeMapper;
    @Autowired
    private ZaMonitorTypeMapper monitorTypeMapper;

    /** key = pfCode + '|' + alias（以及 '*|' + code 兜底：标准编码本身可直接命中） */
    private volatile Map<String, ZaAlarmType> alarmIndex = new HashMap<>();
    private volatile Map<String, ZaDeviceType> deviceIndex = new HashMap<>();
    private volatile Map<String, ZaMonitorType> monitorIndex = new HashMap<>();

    private volatile long lastLoadTime = 0L;

    /** 解析标准告警类型：优先按（插件,别名）命中，其次按标准编码命中 */
    public Optional<ZaAlarmType> resolveAlarmType(String pfCode, String alias) {
        ensureLoaded();
        return resolve(alarmIndex, pfCode, alias);
    }

    /** 解析标准设备类型 */
    public Optional<ZaDeviceType> resolveDeviceType(String pfCode, String alias) {
        ensureLoaded();
        return resolve(deviceIndex, pfCode, alias);
    }

    /** 解析标准监测类型 */
    public Optional<ZaMonitorType> resolveMonitorType(String pfCode, String alias) {
        ensureLoaded();
        return resolve(monitorIndex, pfCode, alias);
    }

    private static <T> Optional<T> resolve(Map<String, T> index, String pfCode, String alias) {
        if (StringUtils.isEmpty(alias)) {
            return Optional.empty();
        }
        T hit = null;
        if (StringUtils.isNotEmpty(pfCode)) {
            hit = index.get(pfCode + "|" + alias);
        }
        if (hit == null) {
            hit = index.get("*|" + alias);
        }
        return Optional.ofNullable(hit);
    }

    /** 类型配置增删改后调用，立即重建索引 */
    public synchronized void reload() {
        try {
            Map<String, ZaAlarmType> alarm = new HashMap<>();
            for (ZaAlarmType t : alarmTypeMapper.selectList(
                    Wrappers.lambdaQuery(ZaAlarmType.class).eq(ZaAlarmType::getStatus, "1"))) {
                index(alarm, t.getCode(), t.getAliases(), t);
            }
            Map<String, ZaDeviceType> device = new HashMap<>();
            for (ZaDeviceType t : deviceTypeMapper.selectList(
                    Wrappers.lambdaQuery(ZaDeviceType.class).eq(ZaDeviceType::getStatus, "1"))) {
                index(device, t.getCode(), t.getAliases(), t);
            }
            Map<String, ZaMonitorType> monitor = new HashMap<>();
            for (ZaMonitorType t : monitorTypeMapper.selectList(
                    Wrappers.lambdaQuery(ZaMonitorType.class).eq(ZaMonitorType::getStatus, "1"))) {
                index(monitor, t.getCode(), t.getAliases(), t);
            }
            this.alarmIndex = alarm;
            this.deviceIndex = device;
            this.monitorIndex = monitor;
            this.lastLoadTime = System.currentTimeMillis();
            log.debug("类型映射索引已刷新: alarm={}, device={}, monitor={}", alarm.size(), device.size(), monitor.size());
        } catch (Exception e) {
            // 类型配置表尚未创建时不影响主流程
            log.warn("类型映射索引加载失败（类型配置表可能未初始化）: {}", e.getMessage());
            this.lastLoadTime = System.currentTimeMillis();
        }
    }

    private static <T> void index(Map<String, T> map, String code, String aliasesJson, T value) {
        // 标准编码本身可作为任意插件的别名命中
        if (StringUtils.isNotEmpty(code)) {
            map.put("*|" + code, value);
        }
        if (StringUtils.isEmpty(aliasesJson)) {
            return;
        }
        try {
            JSONArray arr = JSONArray.parseArray(aliasesJson);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String pf = item.getString("pfCode");
                String alias = item.getString("alias");
                if (StringUtils.isEmpty(alias)) {
                    continue;
                }
                map.put((StringUtils.isEmpty(pf) ? "*" : pf) + "|" + alias, value);
            }
        } catch (Exception e) {
            log.warn("解析别名映射失败 code={}: {}", code, e.getMessage());
        }
    }

    private void ensureLoaded() {
        if (System.currentTimeMillis() - lastLoadTime > REFRESH_INTERVAL_MS) {
            reload();
        }
    }

    /** 全部启用的标准监测类型（供插件按编码取单位等信息） */
    public List<ZaMonitorType> listEnabledMonitorTypes() {
        return monitorTypeMapper.selectList(
                Wrappers.lambdaQuery(ZaMonitorType.class).eq(ZaMonitorType::getStatus, "1"));
    }
}
