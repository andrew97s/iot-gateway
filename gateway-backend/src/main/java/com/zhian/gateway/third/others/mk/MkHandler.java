package com.zhian.gateway.third.others.mk;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.core.message.AlarmPayload;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.TelemetryPayload;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.others.mk.protocol.MkJBProtocol;
import com.zhian.gateway.third.others.mk.vo.MkV3Msg;
import com.zhian.gateway.third.others.mk.constants.MkV3Type;
import com.zhian.gateway.third.others.mk.vo.MkV3Value;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import com.zhian.gateway.third.others.mk.vo.MkJBMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.zhian.gateway.third.others.mk.constants.MkConsts.*;

/**
 * 铭控业务处理器
 *
 * @author tongwenjin
 * @since 2024-12-4
 */

@Component
@Slf4j
public class MkHandler extends BasePlatformHandler<Object> {

    public static String CODE = "mk";

    private final HashMap<String, Long> alarmTimeMap = new HashMap<>();
    /** 设备最近一次报警状态字节，用于边沿检测产生 / 恢复 */
    private final Map<String, Integer> alarmStatusMap = new ConcurrentHashMap<>();

    @Override
    public boolean start(ZaSysPlatform platform) {
        super.start(platform);
        try {
            MkServer.start(this);
        } catch (Exception e) {
            log.error("铭控server启动失败,msg:{}", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean stop() {
        MkServer.stop();
        return super.stop();
    }

    @Override
    public boolean isAlive() {
        if (!MkServer.isActive()) {
            return false;
        }
        checkOfflineDevices();
        return true;
    }

    @Override
    public String getPlatform() {
        return CODE;
    }

    @Override
    public String getProtocol() {
        return CODE;
    }

    @Override
    public R control(ControlVo controlVo) {
        if (controlVo == null || StrUtil.isEmpty(controlVo.getCode())) {
            return R.error("操作失败,设备编码为空!");
        }
        if (!ControlVo.CMD_SET.equals(controlVo.getCommand())) {
            return R.error("操作失败,铭控暂不支持该反控操作!");
        }
        int[] thresholds = parseThresholds(controlVo.getValue());
        if (thresholds == null) {
            return R.error("操作失败,阈值参数非法,示例: {\"high\":800,\"low\":200}");
        }
        byte[] cmd = MkJBProtocol.buildSetCmd(
                controlVo.getCode(), JB_CMD_SET_ALL, thresholds[0], thresholds[1]
        );
        if (!MkServer.send(controlVo.getCode(), cmd)) {
            return R.error("操作失败,设备不在线!");
        }
        return R.success("阈值下发成功");
    }

    @Override
    public ProcessInfo doProcessMsg(Object msg) {
        if (msg instanceof MkV3Msg) {
            processV3Msg((MkV3Msg) msg);
            return null;
        }
        if (msg instanceof MkJBMsg) {
            processJbMsg((MkJBMsg) msg);
            return null;
        }
        return null;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        int count = checkOfflineDevices();
        return DeviceSyncInfo.success(count);
    }

    private void processV3Msg(MkV3Msg msg) {
        if (msg.getCommand() != 0x00) {
            log.info("收到铭控V3非上发帧, cmd={}, code={}", msg.getCommand(), msg.getDeviceCode());
            return;
        }
        ZaSysDevice device = syncV3Device(msg);
        MessageUtil.setDevice(device);
        if (MsgProcessContext.getProcessInfo() != null) {
            MsgProcessContext.getProcessInfo().setDevice(device);
        }
        if (device.getId() != null) {
            statusMap.put(device.getId(), System.currentTimeMillis());
        }
        String raw = JSON.toJSONString(msg);

        // 监测数据
        extractV3Telemetry(device, msg);
        // 告警
        extractV3Alarms(device, msg, raw);
    }

    private void extractV3Telemetry(ZaSysDevice device, MkV3Msg msg) {
        List<TelemetryPayload.Telemetry> telemetries = new ArrayList<>();
        addTelemetry(
                telemetries,
                "battery",
                String.valueOf(msg.getBatteryPercent()),
                "电池电量", 0, null, null, "%"
        );
        addTelemetry(
                telemetries,
                "rssi",
                String.valueOf(msg.getRssi()),
                "信号强度", 0, null, null, "dBm"
        );

        List<MkV3Value> values = msg.getCurrentValues();
        if (values != null) {
            for (MkV3Value value : values) {
                if (value == null || StrUtil.isBlank(value.getValue())) {
                    continue;
                }
                String alias = StrUtil.blankToDefault(value.getMonitorAlias(), value.getName());
                String desc = StrUtil.blankToDefault(value.getName(), alias);
                addTelemetry(
                        telemetries, alias, value.getValue(), desc,
                        value.getChannel(), value.getThresholdLow(), value.getThresholdHigh(), value.getUnit()
                );
            }
        }
        if (telemetries.isEmpty()) {
            return;
        }
        Message message = MessageBuilder.buildTelemetry(device, telemetries);
        if (msg.getTime() != null) {
            message.setTimestamp(msg.getTime().getTime());
            for (TelemetryPayload.Telemetry telemetry : telemetries) {
                telemetry.setTimestamp(msg.getTime().getTime());
            }
        }
        MsgProcessContext.addMsg(message);
    }

    private void addTelemetry(
            List<TelemetryPayload.Telemetry> telemetries, String alias, String value, String desc,
            int channel, String thresholdLow, String thresholdHigh, String unit
    ) {
        Optional<ZaMonitorType> type = typeMappingService.resolveMonitorType(getPlatform(), alias);
        if (!type.isPresent() && StrUtil.isNotBlank(unit)) {
            type = typeMappingService.resolveMonitorType(getPlatform(), unit);
        }
        if (!type.isPresent()) {
            log.debug("铭控监测类型未注册, alias={}, unit={}", alias, unit);
            return;
        }
        TelemetryPayload.Telemetry telemetry = MessageBuilder.builderTelemetry(
                type.get(), value, desc, channel, thresholdLow, thresholdHigh, unit
        );
        if (telemetry != null) {
            telemetries.add(telemetry);
        }
    }

    /**
     * 按表三报警状态字节做边沿检测：位 0→1 产生，1→0 恢复。
     */
    private void extractV3Alarms(ZaSysDevice device, MkV3Msg msg, String raw) {
        int current = msg.getAlarmStatus() & 0xFF;
        int previous = alarmStatusMap.getOrDefault(device.getCode(), 0);
        alarmStatusMap.put(device.getCode(), current);
        int changed = previous ^ current;
        if (changed == 0) {
            return;
        }
        String[] codes = v3AlarmCodes(msg.getDeviceType());
        for (int bit = 0; bit < 8; bit++) {
            if ((changed & (1 << bit)) == 0) {
                continue;
            }
            String alarmCode = codes[bit];
            if (StrUtil.isBlank(alarmCode)) {
                continue;
            }
            boolean active = (current & (1 << bit)) != 0;
            pushAlarmState(device, alarmCode, active, raw);
        }
    }

    /**
     * 表三默认位 + 文档注 1~8 的类型覆盖。
     */
    private String[] v3AlarmCodes(int type) {
        String[] codes = new String[]{
                ALARM_LOW, ALARM_HIGH, ALARM_LOW_BATTERY, ALARM_SENSOR_FAULT,
                ALARM_DEVICE_FAULT, ALARM_LOW, ALARM_HIGH, null
        };
        switch (type) {
            // 消火栓
            case 24:
                codes[4] = ALARM_DEVICE_FAULT;
                codes[5] = ALARM_SHOCK;
                codes[6] = ALARM_TILT;
                codes[7] = ALARM_WATER_DISCHARGE;
                break;
            // 消火栓压力
            case 34:
                codes[7] = ALARM_WATER_DISCHARGE;
                break;
            // 消火栓闷盖设备
            case 36:
                codes[0] = ALARM_LOW;
                codes[1] = null;
                codes[5] = ALARM_SHOCK;
                codes[6] = ALARM_COVER_OPEN;
                break;
            // 温度异动设备
            case 39:
                codes[5] = ALARM_SHOCK;
                break;
            // 井盖液位设备
            case 40:
                codes[7] = ALARM_WELL_OPEN;
                break;
            // 消火栓流量
            case 58:
                codes[5] = ALARM_SHOCK;
                codes[6] = ALARM_TILT;
                codes[7] = ALARM_WATER_DISCHARGE;
                break;
            // 智能井盖监测终端
            case 67:
                codes[0] = ALARM_LOW;
                codes[1] = null;
                codes[5] = ALARM_SHOCK;
                codes[6] = ALARM_TILT;
                break;
            // 智能空气质量监测终端
            case 79:
            case 96:
            case 105:
                codes[2] = ALARM_DEVICE_FAULT;
                codes[3] = ALARM_SENSOR_FAULT;
                codes[7] = ALARM_COVER_OPEN;
                break;
            default:
                break;
        }
        return codes;
    }

    private void pushAlarmState(ZaSysDevice device, String alarmCode, boolean active, String raw) {
        if (device == null) {
            return;
        }
        Optional<ZaAlarmType> type = typeMappingService.resolveAlarmType(getPlatform(), alarmCode);
        if (!type.isPresent()) {
            log.error("铭控告警类型{}未注册!", alarmCode);
            return;
        }

        // TODO 告警产生 & 恢复对应 两个事件
        String state = active ? AlarmPayload.STATE_ACTIVE : AlarmPayload.STATE_RECOVERED;
        String desc = type.get().getName() + (active ? " 产生" : " 恢复");
        MsgProcessContext.addMsg(
                MessageBuilder.buildAlarm(device, type.get(), desc, "", state)
        );
        log.info("铭控设备({})告警{} {}", device.getCode(), alarmCode, state);
    }

    private ZaSysDevice syncV3Device(MkV3Msg msg) {
        MkV3Type type = MkV3Type.of(msg.getDeviceType());
        String typeCode = type == null ? DEVICE_TYPE_PRESSURE : type.getTypeCode();
        String typeName = type == null ? "MK-V3" : type.getName();
        String model = type == null ? "MK-V3" : "MK-" + type.getCode();
        return DeviceUtil.syncDevice(
                SyncDevice.builder()
                        .code(msg.getDeviceCode())
                        .pfCode(CODE)
                        .ip(msg.getSourceIp())
                        .typeCode(typeCode)
                        .model(model)
                        .wireless(Constants.YES)
                        .online(DictValue.DEVICE_ONLINE)
                        .name(typeName + "-" + msg.getDeviceCode())
                        .remark(typeName)
                        .build()
        );
    }

    private void processJbMsg(MkJBMsg msg) {
        if (msg.getDataType() == JB_TYPE_ACK) {
            log.info("收到铭控青鸟协议设备应答, code={}", msg.getCode());
            return;
        }
        ZaSysDevice device = syncJbDevice(msg);
        String raw = JSON.toJSONString(msg);
        int dataType = msg.getDataType();
        switch (dataType) {
            case JB_TYPE_HEARTBEAT:
                break;
            case JB_TYPE_LOW_BATTERY:
                pushAlarmIfNeeded(device, ALARM_LOW_BATTERY, raw);
                break;
            case JB_TYPE_FAULT:
                pushAlarmIfNeeded(device, ALARM_DEVICE_FAULT, raw);
                break;
            case JB_TYPE_PERIOD:
            case JB_TYPE_CHANGE:
            case JB_TYPE_PERIOD_CM:
            case JB_TYPE_CHANGE_CM:
                consumeMsg(MqMessage.createBusiness(device, raw));
                break;
            default:
                consumeMsg(MqMessage.createBusiness(device, raw));
        }
    }

    private void pushJbValueAlarmIfNeeded(ZaSysDevice device, MkJBMsg msg, String raw) {
        if (KIND_PUMP.equals(msg.getKind())) {
            if (msg.getDataType() == JB_TYPE_CHANGE) {
                pushAlarmIfNeeded(device, "1".equals(msg.getValue()) ? ALARM_PUMP_ON : ALARM_PUMP_OFF, raw);
            }
            return;
        }
        Integer value = parseInt(msg.getValue());
        Integer high = parseInt(msg.getThresholdHigh());
        Integer low = parseInt(msg.getThresholdLow());
        if (value == null) {
            return;
        }
        // 液位类型 5/6 当前值为厘米，阈值仍为毫米
        if ("cm".equalsIgnoreCase(msg.getUnit())) {
            value = value * 10;
        }
//        if (high != null && high > 0 && value > high) {
//            pushAlarmIfNeeded(device, ALARM_HIGH, raw);
//        }
//        if (low != null && value < low) {
//            pushAlarmIfNeeded(device, ALARM_LOW, raw);
//        }
    }

    private void pushAlarmIfNeeded(ZaSysDevice device, String alarmCode, String raw) {
        if (device == null) {
            return;
        }
        String alarmKey = device.getCode() + "_" + alarmCode;
        Long lastAlarmTime = alarmTimeMap.getOrDefault(alarmKey, 0L);
        // 同一设备 2小时内不允许出现重复的告警
        if (lastAlarmTime < DateUtil.offsetHour(new Date(), -2).getTime()) {
            alarmTimeMap.put(alarmKey, new Date().getTime());
            Optional<ZaAlarmType> type = typeMappingService.resolveAlarmType(getPlatform(), alarmCode);
            if (type.isPresent()) {
                MsgProcessContext.addMsg(
                        MessageBuilder.buildAlarm(device, type.get(), type.get().getName(), "")
                );
            } else {
                log.error("铭控告警类型{}未注册!", alarmCode);
            }
        } else {
            log.warn("铭控设备({})在2小时内出现了重复的告警,已忽略!", device.getCode());
        }
    }


    private ZaSysDevice syncJbDevice(MkJBMsg msg) {
        return DeviceUtil.syncDevice(
                SyncDevice.builder()
                        .code(msg.getCode())
                        .pfCode(CODE)
                        .ip(msg.getSourceIp())
                        .typeCode("4GWMETER")
                        .model("JBF-VS31C")
                        .wireless(Constants.YES)
                        .name(msg.getCode())
                        .remark(JSON.toJSONString(msg))
                        .build()
        );
    }

    private String resolveJbType(MkJBMsg msg) {
        if (KIND_PUMP.equals(msg.getKind())) {
            return DEVICE_TYPE_PUMP;
        }
        if (KIND_LEVEL.equals(msg.getKind())) {
            return DEVICE_TYPE_LEVEL;
        }
        return DEVICE_TYPE_PRESSURE;
    }


    private int[] parseThresholds(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            if (value.trim().startsWith("{")) {
                JSONObject json = JSON.parseObject(value);
                Integer high = json.getInteger("high");
                Integer low = json.getInteger("low");
                if (high == null || low == null) {
                    return null;
                }
                return new int[]{high, low};
            }
            String[] parts = value.split("[,，]");
            if (parts.length != 2) {
                return null;
            }
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 超过 60 分钟无报文则离线，由平台定时任务调用 {@link #isAlive()} / {@link #syncDeviceStatus()} 巡检
     */
    private int checkOfflineDevices() {
        ZaSysDevice query = new ZaSysDevice();
        query.setPfCode(getPlatform());
        query.setOnline(DictValue.DEVICE_ONLINE);
        List<ZaSysDevice> deviceList = deviceService.selectZaSysDeviceList(query);
        if (deviceList == null || deviceList.isEmpty()) {
            return 0;
        }
        int offlineCount = 0;
        Date now = new Date();
        for (ZaSysDevice device : deviceList) {
            Long lastHeartTime = DeviceUtil.getCommTime(device.getId());
            if (lastHeartTime == null) {
                lastHeartTime = statusMap.get(device.getId());
            }
            if (lastHeartTime == null) {
                continue;
            }
            if (DateUtil.between(now, new Date(lastHeartTime), DateUnit.MINUTE) > 60) {
                log.info("铭控设备({})超过60分钟无通讯, 标记为离线", device.getCode());
                device.setOnline(DictValue.DEVICE_OFFLINE);
                deviceService.updateZaSysDevice(device);
                if (MsgProcessContext.getProcessInfo() != null) {
                    MsgProcessContext.addMsg(
                            MessageBuilder.buildDeviceState(device, DictValue.DEVICE_OFFLINE, "超过60分钟无通讯")
                    );
                }
                offlineCount++;
            }
        }
        return offlineCount;
    }

    private Integer parseInt(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
