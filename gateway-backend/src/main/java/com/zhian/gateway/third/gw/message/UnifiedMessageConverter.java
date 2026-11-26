package com.zhian.gateway.third.gw.message;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaDeviceType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.system.service.ISysConfigService;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/**
 * 统一消息转换器
 *
 * 将各接入插件产出的 {@link MqMessage}（原始事件）转换为统一消息 {@link UnifiedMessage}：
 * 1. 厂商告警码 → 标准告警类型（za_alarm_type 别名映射）
 * 2. 厂商设备类型 → 标准设备类型（za_device_type 别名映射）
 * 3. 监测项（信号强度/电量/电压/温度及厂商自定义项）→ 标准监测类型（za_monitor_type）
 *
 * 转换集中在同步上级平台的边界完成，各插件无需感知标准类型字典。
 */
@Slf4j
@Component
public class UnifiedMessageConverter {

    /** 内置监测项别名（Facility 固有字段） */
    public static final String METRIC_RSSI = "rssi";
    public static final String METRIC_VOLTAGE = "voltage";
    public static final String METRIC_TEMPERATURE = "temperature";
    public static final String METRIC_ONLINE = "online";

    @Autowired
    private TypeMappingService typeMappingService;

    @Autowired
    private ISysConfigService configService;

    /**
     * MqMessage → 统一消息
     */
    public UnifiedMessage convert(MqMessage mq) {
        UnifiedMessage msg = new UnifiedMessage();
        // 幂等键：沿用插件生成的消息UUID，重推时保持不变
        msg.setMessageId(StringUtils.isNotEmpty(mq.getUuid())
                ? mq.getUuid() : SnowflakeIdWorker.getInstance().nextStringId());
        msg.setTime(mq.getTime() == null ? new Date() : mq.getTime());
        msg.setGatewayCode(gatewayCode());
        msg.setDevice(buildDevice(mq));

        String event = mq.getEvent() == null ? "" : mq.getEvent();
        switch (event) {
            case MqMessage.EVENT_ALARM:
                msg.setMsgType(UnifiedMessage.TYPE_ALARM);
                fillAlarmPayload(msg, mq);
                break;
            case MqMessage.EVENT_BUSINESS:
                msg.setMsgType(UnifiedMessage.TYPE_MONITOR);
                fillMonitorPayload(msg, mq);
                break;
            case MqMessage.EVENT_DEVICE:
                msg.setMsgType(UnifiedMessage.TYPE_DEVICE);
                fillDevicePayload(msg, mq);
                break;
            default:
                // 其它事件（级联响应等）按原类型透传
                msg.setMsgType(event);
                msg.getPayload().put("raw", mq.getMsgData());
        }
        return msg;
    }

    private String gatewayCode() {
        try {
            String code = configService.selectConfigByKey("gateway.code");
            return StringUtils.isEmpty(code) ? "GW-0001" : code;
        } catch (Exception e) {
            return "GW-0001";
        }
    }

    private String pfCodeOf(MqMessage mq) {
        if (mq.getFacility() != null && StringUtils.isNotEmpty(mq.getFacility().getPfCode())) {
            return mq.getFacility().getPfCode();
        }
        return mq.getProtocol();
    }

    private UnifiedMessage.DeviceInfo buildDevice(MqMessage mq) {
        UnifiedMessage.DeviceInfo device = new UnifiedMessage.DeviceInfo();
        MqMessage.Facility f = mq.getFacility();
        if (f == null) {
            return device;
        }
        device.setCode(f.getCode());
        device.setName(f.getName());
        device.setModel(f.getModel());
        device.setNet(f.getNet());
        device.setPfCode(pfCodeOf(mq));
        device.setOnline(f.isOnline());
        device.setRawType(f.getType());
        // 厂商设备类型 → 标准设备类型
        Optional<ZaDeviceType> std = typeMappingService.resolveDeviceType(pfCodeOf(mq), f.getType());
        if (std.isPresent()) {
            device.setType(std.get().getCode());
            device.setTypeName(std.get().getName());
        } else {
            device.setType(f.getType());
        }
        return device;
    }

    // ==================== 告警 ====================
    private void fillAlarmPayload(UnifiedMessage msg, MqMessage mq) {
        String rawAlarmType = mq.getEventType();
        String alarmStatus = UnifiedMessage.ALARM_STATUS_OCCUR;
        String cancelReason = null;
        // 约定：插件可用 "cancel:<原因>|<告警码>" 表达告警撤销
        if (rawAlarmType != null && rawAlarmType.startsWith("cancel:")) {
            alarmStatus = UnifiedMessage.ALARM_STATUS_CANCEL;
            String rest = rawAlarmType.substring("cancel:".length());
            int idx = rest.indexOf('|');
            if (idx >= 0) {
                cancelReason = rest.substring(0, idx);
                rawAlarmType = rest.substring(idx + 1);
            } else {
                rawAlarmType = rest;
            }
        }

        msg.getPayload().put("rawAlarmType", rawAlarmType);
        Optional<ZaAlarmType> std = typeMappingService.resolveAlarmType(pfCodeOf(mq), rawAlarmType);
        if (std.isPresent()) {
            msg.getPayload().put("alarmType", std.get().getCode());
            msg.getPayload().put("alarmTypeName", std.get().getName());
            msg.getPayload().put("level", std.get().getType());
        } else {
            // 未配置映射时保留原始告警码，方便上级平台与运维排查
            msg.getPayload().put("alarmType", rawAlarmType);
        }
        msg.getPayload().put("alarmTime", format(msg.getTime()));
        msg.getPayload().put("status", alarmStatus);
        if (cancelReason != null) {
            msg.getPayload().put("cancelReason", cancelReason);
        }
        if (StringUtils.isNotEmpty(mq.getImageUrl())) {
            msg.getPayload().put("imageUrl", mq.getImageUrl());
        }
        msg.getPayload().put("raw", mq.getMsgData());
    }

    // ==================== 监测 ====================
    private void fillMonitorPayload(UnifiedMessage msg, MqMessage mq) {
        String pfCode = pfCodeOf(mq);
        MqMessage.Facility f = mq.getFacility();
        if (f != null) {
            addMetric(msg, pfCode, METRIC_ONLINE, f.isOnline() ? "1" : "0");
            if (f.getRssi() != null) {
                addMetric(msg, pfCode, METRIC_RSSI, f.getRssi());
            }
            if (f.getVoltage() != null) {
                addMetric(msg, pfCode, METRIC_VOLTAGE, f.getVoltage());
            }
            if (f.getTemperature() != null) {
                addMetric(msg, pfCode, METRIC_TEMPERATURE, f.getTemperature());
            }
        }
        // 厂商自定义监测项：约定 msgData 为 {"metrics":{"别名":值,...}} 时逐项映射
        if (StringUtils.isNotEmpty(mq.getMsgData())) {
            try {
                JSONObject data = JSONObject.parseObject(mq.getMsgData());
                JSONObject metrics = data.getJSONObject("metrics");
                if (metrics != null) {
                    for (String alias : metrics.keySet()) {
                        addMetric(msg, pfCode, alias, metrics.get(alias));
                    }
                }
            } catch (Exception ignore) {
                // msgData 非JSON时忽略，原文透传
            }
        }
        msg.getPayload().put("monitorTime", format(msg.getTime()));
        msg.getPayload().put("raw", mq.getMsgData());
    }

    private void addMetric(UnifiedMessage msg, String pfCode, String alias, Object value) {
        Optional<ZaMonitorType> std = typeMappingService.resolveMonitorType(pfCode, alias);
        if (std.isPresent()) {
            ZaMonitorType t = std.get();
            msg.addMetric(UnifiedMessage.Metric.of(
                    t.getCode(), t.getName(), t.getValueType(), t.getUnit(), value,
                    resolveEnumLabel(t, value)));
        } else {
            // 未配置映射时按别名透传
            msg.addMetric(UnifiedMessage.Metric.of(alias, null, null, null, value, null));
        }
    }

    private static String resolveEnumLabel(ZaMonitorType type, Object value) {
        if (!ZaMonitorType.VALUE_TYPE_ENUM.equals(type.getValueType())
                || StringUtils.isEmpty(type.getEnumOptions()) || value == null) {
            return null;
        }
        try {
            JSONArray options = JSONArray.parseArray(type.getEnumOptions());
            for (int i = 0; i < options.size(); i++) {
                JSONObject opt = options.getJSONObject(i);
                if (String.valueOf(value).equals(opt.getString("value"))) {
                    return opt.getString("label");
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    // ==================== 设备 ====================
    private void fillDevicePayload(UnifiedMessage msg, MqMessage mq) {
        String action;
        String eventType = mq.getEventType();
        if (MqMessage.DEVICE_ADD.equals(eventType)) {
            action = UnifiedMessage.DEVICE_ACTION_ADD;
        } else if (MqMessage.DEVICE_REMOVE.equals(eventType)) {
            action = UnifiedMessage.DEVICE_ACTION_DELETE;
        } else if (MqMessage.DEVICE_UPDATE.equals(eventType)) {
            action = UnifiedMessage.DEVICE_ACTION_UPDATE;
        } else {
            action = StringUtils.isEmpty(eventType) ? UnifiedMessage.DEVICE_ACTION_QUERY : eventType;
        }
        msg.getPayload().put("action", action);
        msg.getPayload().put("raw", mq.getMsgData());
    }

    private static String format(Date date) {
        return com.zhian.gateway.common.utils.DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", date);
    }
}
