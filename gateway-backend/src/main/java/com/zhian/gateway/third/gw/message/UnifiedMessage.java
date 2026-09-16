package com.zhian.gateway.third.gw.message;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关统一消息格式（同步上级平台的标准信封）
 *
 * <pre>
 * {
 *   "messageId": "全局唯一ID（幂等去重键）",
 *   "msgType": "device | alarm | monitor | control",
 *   "time": "yyyy-MM-dd HH:mm:ss",
 *   "gatewayCode": "GW-0001",
 *   "device": { "code","name","type","rawType","model","pfCode","net","online" },
 *   "payload": { 按 msgType 定义 }
 * }
 * </pre>
 *
 * payload 定义：
 * - device : { action: add|update|delete|query }
 * - alarm  : { alarmType, alarmTypeName, level, alarmTime, status: occur|cancel, cancelReason, imageUrl }
 * - monitor: { metrics: [{ monitorType, monitorTypeName, valueType, unit, value, valueLabel }], monitorTime }
 * - control: { command, params, result }（上级平台下发 / 网关回执）
 */
@Data
public class UnifiedMessage {

    public static final String TYPE_DEVICE = "device";
    public static final String TYPE_ALARM = "alarm";
    public static final String TYPE_MONITOR = "monitor";
    public static final String TYPE_CONTROL = "control";

    /** 设备动作 */
    public static final String DEVICE_ACTION_ADD = "add";
    public static final String DEVICE_ACTION_UPDATE = "update";
    public static final String DEVICE_ACTION_DELETE = "delete";
    public static final String DEVICE_ACTION_QUERY = "query";

    /** 告警状态 */
    public static final String ALARM_STATUS_OCCUR = "occur";
    public static final String ALARM_STATUS_CANCEL = "cancel";

    /** 消息唯一ID（上级平台按此去重，保证幂等） */
    private String messageId;

    /** 消息类型：device / alarm / monitor / control */
    private String msgType;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date time;

    /** 网关编码 */
    private String gatewayCode;

    /** 设备信息 */
    private DeviceInfo device;

    /** 消息体 */
    private Map<String, Object> payload = new LinkedHashMap<>();

    public String toJson() {
        return JSON.toJSONString(this);
    }

    @Data
    public static class DeviceInfo {
        /** 设备编码 */
        private String code;
        /** 设备名称/位置 */
        private String name;
        /** 标准设备类型编码（za_device_type.code） */
        private String type;
        /** 标准设备类型名称 */
        private String typeName;
        /** 插件原始类型（别名，用于排查） */
        private String rawType;
        /** 设备型号 */
        private String model;
        /** 来源插件 */
        private String pfCode;
        /** 网关代码 */
        private String net;
        /** 是否在线 */
        private Boolean online;
    }

    /** 监测项 */
    @Data
    public static class Metric {
        /** 标准监测类型编码（za_monitor_type.code） */
        private String monitorType;
        /** 标准监测类型名称 */
        private String monitorTypeName;
        /** enum / linear */
        private String valueType;
        /** 单位（线性值） */
        private String unit;
        /** 监测值 */
        private Object value;
        /** 枚举值文本（枚举类型时） */
        private String valueLabel;

        public static Metric of(String type, String typeName, String valueType, String unit, Object value, String valueLabel) {
            Metric m = new Metric();
            m.setMonitorType(type);
            m.setMonitorTypeName(typeName);
            m.setValueType(valueType);
            m.setUnit(unit);
            m.setValue(value);
            m.setValueLabel(valueLabel);
            return m;
        }
    }

    /** 便捷方法：向 monitor payload 追加监测项 */
    @SuppressWarnings("unchecked")
    public void addMetric(Metric metric) {
        List<Metric> metrics = (List<Metric>) payload.computeIfAbsent("metrics", k -> new ArrayList<Metric>());
        metrics.add(metric);
    }
}
