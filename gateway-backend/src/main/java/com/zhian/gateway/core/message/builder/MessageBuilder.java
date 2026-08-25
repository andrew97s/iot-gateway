package com.zhian.gateway.core.message.builder;

import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.consts.MessageConstants;
import com.zhian.gateway.core.message.*;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.vo.ControlVo;

import java.util.Collections;
import java.util.List;

/**
 * 全息网关消息builder
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
public class MessageBuilder {

    public static Message buildDevice(ZaSysDevice device, String action, String reason) {
        Message message = buildBasic(device, action);
        DevicePayload payload = new DevicePayload();

        // 设备的增删改查payload
        payload.setAction(action);
        payload.setDeviceId(device.getId());
        payload.setCode(device.getCode());
        payload.setNet(device.getNet());
        payload.setTypeCode(device.getType());
        payload.setName(device.getName());
        payload.setOnline(device.getOnline());
        payload.setIp(device.getIp());
        payload.setReason(reason);
        message.setPayload(payload);

        return message;
    }

    public static Message buildDeviceState(ZaSysDevice device, String online, String reason) {
        Message message = buildBasic(device, MessageConstants.MSG_TYPE_ALARM);

        // 告警
        AlarmPayload payload = new AlarmPayload();
        payload.setTimestamp(System.currentTimeMillis());
        payload.setCode(StrUtil.equals(online, "1") ? "54" : "55");
        payload.setName("设备" + (StrUtil.equals(online, "1") ? "在线" : "离线"));
        payload.setType(3);
        payload.setDesc(reason);
        message.setPayload(payload);

        return message;
    }

    public static Message buildAlarm(
            ZaSysDevice device, ZaAlarmType alarm, String desc, String picUrl
    ) {
        Message message = buildBasic(device, MessageConstants.MSG_TYPE_ALARM);
        AlarmPayload payload = new AlarmPayload();

        // 告警
        payload.setTimestamp(System.currentTimeMillis());
        payload.setType(alarm.getType());
        payload.setCode(alarm.getCode());
        payload.setName(alarm.getName());
        payload.setDesc(desc);
        payload.setPicUrl(picUrl);
        message.setPayload(payload);

        return message;
    }

    public static Message buildControl(
            ZaSysDevice device, ControlVo controlVo
    ) {
        Message message = buildBasic(device, MessageConstants.MSG_TYPE_CONTROL);
        ControlPayload payload = new ControlPayload();

        // 告警
        payload.setCmd(controlVo.getCommand());
        payload.setParams(controlVo.getValue());
        payload.setResult("result");
        message.setPayload(payload);

        return message;
    }

    public static Message buildTelemetry(ZaSysDevice device, List<TelemetryPayload.Telemetry> telemetries) {
        Message message = buildBasic(device, MessageConstants.MSG_TYPE_TELEMETRY);

        // 监测值
        TelemetryPayload payload = new TelemetryPayload();
        payload.setTelemetries(telemetries);
        message.setPayload(payload);

        return message;
    }

    public static Message buildTelemetry(
            ZaSysDevice device, ZaMonitorType monitorType, String value, String desc
    ) {
        Message message = buildBasic(device, MessageConstants.MSG_TYPE_TELEMETRY);

        // 监测值
        TelemetryPayload payload = new TelemetryPayload();

        payload.setTelemetries(Collections.singletonList(builderTelemetry(monitorType, value, desc)));
        message.setPayload(payload);

        return message;
    }

    public static TelemetryPayload.Telemetry builderTelemetry(ZaMonitorType monitorType, String value, String desc) {
        if (monitorType == null) {
            return null;
        }

        // 监测值
        TelemetryPayload.Telemetry.TelemetryBuilder builder = TelemetryPayload.Telemetry.builder();

        builder.code(monitorType.getCode());
        builder.name(monitorType.getName());
        builder.value(value);
        builder.unit(monitorType.getUnit());
        builder.timestamp(System.currentTimeMillis());
        builder.desc(StrUtil.isNotBlank(desc) ? desc : monitorType.getName());

        return builder.build();
    }


    private static Message buildBasic(ZaSysDevice device, String msgType) {
        Message message = new Message();

        // 基础信息
        message.setId(SnowflakeIdWorker.getInstance().nextStringId());
        message.setType(msgType);

        // TODO 网关ID由设备硬件地址计算出来的一个常量
        message.setGatewayCode("test_gateway_v2.0");
        message.setTimestamp(System.currentTimeMillis());

        // 设备信息
        MessageDevice msgDevice = new MessageDevice();
        msgDevice.setDeviceId(device.getId());
        msgDevice.setCode(device.getCode());
        msgDevice.setName(device.getName());
        msgDevice.setNet(device.getNet());
        msgDevice.setTypeCode(device.getType());
        msgDevice.setOnline(device.getOnline());
        message.setDevice(msgDevice);

        return message;
    }
}
