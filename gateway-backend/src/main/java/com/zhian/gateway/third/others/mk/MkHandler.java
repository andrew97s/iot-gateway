package com.zhian.gateway.third.others.mk;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.TelemetryPayload;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.others.mk.constants.MkConsts;
import com.zhian.gateway.third.others.mk.protocol.MkJBProtocol;
import com.zhian.gateway.third.others.mk.vo.MkV3Msg;
import com.zhian.gateway.third.others.mk.constants.MkV3Type;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import com.zhian.gateway.third.others.mk.vo.MkJBMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

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

    private HashMap<String, Long> alarmTimeMap = new HashMap<>();

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

    private void processV3Msg(MkV3Msg msg) {
        if (msg.getCommand() != 0x00) {
            log.info("收到铭控V3非上发帧, cmd={}, code={}", msg.getCommand(), msg.getDeviceCode());
            return;
        }
        ZaSysDevice device = syncV3Device(msg);
        String raw = JSON.toJSONString(msg);

        TypeMappingService mappingService = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaMonitorType> type = mappingService.resolveMonitorType(getPlatform(), msg.getUnit());
        if (type.isPresent()) {
            // 电池电量
            TelemetryPayload.Telemetry battery = MessageBuilder.builderTelemetry(
                    fetchMonitorType("battery"), msg.getBatteryPercent() + "", "电池电量"
            );
            TelemetryPayload.Telemetry rssi = MessageBuilder.builderTelemetry(
                    fetchMonitorType("rssi"), msg.getRssi() + "", "信号强度"
            );
            // 监测数据
            List<String> values = msg.getCurrentValues();
            for (String value : values) {

            }
            Message message = MessageBuilder.buildTelemetry(device, Arrays.asList(battery, rssi));
        }

        consumeMsg(MqMessage.createBusiness(device, raw));
        pushV3Alarms(device, msg, raw);
    }

    private void pushV3Alarms(ZaSysDevice device, MkV3Msg msg, String raw) {
        int status = msg.getAlarmStatus();
        if (status == 0) {
            return;
        }
        int type = msg.getDeviceType();
        // 消火栓（压力） 、 消火栓（压力、倾角）、消火栓（压力、倾角、流量、温度）
        if (type == 34 || type == 24 || type == 58) {
            if ((status & 0x80) != 0) {
                pushAlarmIfNeeded(device, ALARM_WATER_DISCHARGE, raw);
            }
        }
        // 消火栓（压力、倾角）、消火栓（压力、倾角、流量、温度）
        if (type == 24 || type == 58) {
            if ((status & 0x40) != 0) {
                pushAlarmIfNeeded(device, ALARM_TILT, raw);
            }
            if ((status & 0x20) != 0) {
                pushAlarmIfNeeded(device, ALARM_SHOCK, raw);
            }
        }
        // 消防栓闷盖（①水浸状态、②闷盖状态）
        if (type == 36) {
            if ((status & 0x01) != 0) {
                pushAlarmIfNeeded(device, ALARM_LOW, raw);
            }
            if ((status & 0x40) != 0) {
                pushAlarmIfNeeded(device, ALARM_COVER_OPEN, raw);
            }
            if ((status & 0x20) != 0) {
                pushAlarmIfNeeded(device, ALARM_SHOCK, raw);
            }
            return;
        }
        // 井盖液位
        if (type == 40 && (status & 0x80) != 0) {
            pushAlarmIfNeeded(device, ALARM_WELL_OPEN, raw);
        }
        // 智能井盖监测终端
        if (type == 67) {
            if ((status & 0x01) != 0) {
                pushAlarmIfNeeded(device, ALARM_LOW, raw);
            }
            if ((status & 0x40) != 0) {
                pushAlarmIfNeeded(device, ALARM_TILT, raw);
            }
            if ((status & 0x20) != 0) {
                pushAlarmIfNeeded(device, ALARM_SHOCK, raw);
            }
            return;
        }
        // 温度&异动（①温度、②异动）
        if (type == 39 && (status & 0x20) != 0) {
            pushAlarmIfNeeded(device, ALARM_SHOCK, raw);
        }
        if ((status & 0x01) != 0) {
            pushAlarmIfNeeded(device, ALARM_LOW, raw);
        }
        if ((status & 0x02) != 0) {
            pushAlarmIfNeeded(device, ALARM_HIGH, raw);
        }
        if ((status & 0x04) != 0) {
            pushAlarmIfNeeded(device, ALARM_LOW_BATTERY, raw);
        }
        if ((status & 0x08) != 0) {
            pushAlarmIfNeeded(device, ALARM_SENSOR_FAULT, raw);
        }
        if ((status & 0x10) != 0) {
            pushAlarmIfNeeded(device, ALARM_DEVICE_FAULT, raw);
        }
        if ((status & 0x20) != 0 && type != 24 && type != 58 && type != 39) {
            pushAlarmIfNeeded(device, ALARM_LOW, raw);
        }
        if ((status & 0x40) != 0 && type != 24 && type != 58) {
            pushAlarmIfNeeded(device, ALARM_HIGH, raw);
        }
    }

    private ZaSysDevice syncV3Device(MkV3Msg msg) {
        MkV3Type type = MkV3Type.of(msg.getDeviceType());
        String typeCode = type == null ? DEVICE_TYPE_PRESSURE : type.getTypeCode();
        String model = type == null ? "MK-V3" : "MK-" + type.getCode();
        return DeviceUtil.syncDevice(
                SyncDevice.builder()
                        .code(msg.getDeviceCode())
                        .pfCode(CODE)
                        .ip(msg.getSourceIp())
                        .typeCode(typeCode)
                        .model(model)
                        .wireless(Constants.YES)
                        .name(msg.getDeviceCode())
                        .remark(JSON.toJSONString(msg))
                        .build()
        );
    }

    private void processJbMsg(MkJBMsg msg) {
        if (msg.getDataType() == JB_TYPE_ACK) {
            log.info("收到铭控青鸟协议设备应答, code={}", msg.getCode());
            return;
        }
        ZaSysDevice device = syncJbDevice(msg);
//        autoOnline(device);
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
//                pushJbValueAlarmIfNeeded(device, msg, raw);
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
            Optional<ZaAlarmType> type = SpringUtils.getBean(TypeMappingService.class).resolveAlarmType(getPlatform(), alarmCode);
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
     * 超过 60 分钟无报文则离线，由平台定时任务调用 {@link #isAlive()} 巡检
     */
    private void checkOfflineDevices() {
        ZaSysDevice query = new ZaSysDevice();
        query.setPfCode(getPlatform());
        query.setOnline(DictValue.DEVICE_ONLINE);
        List<ZaSysDevice> deviceList = deviceService.selectZaSysDeviceList(query);
        if (deviceList == null || deviceList.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (ZaSysDevice device : deviceList) {
            long lastHeartTime = statusMap.getOrDefault(device.getId(), System.currentTimeMillis());
            if (DateUtil.between(now, new Date(lastHeartTime), DateUnit.MINUTE) > 60) {
                log.info("铭控设备({})超过60分钟无通讯, 标记为离线", device.getCode());
                device.setOnline(DictValue.DEVICE_OFFLINE);
                deviceService.updateZaSysDevice(device);
//                pushState(device, AlarmType.JB_OFFLINE.getCode());
            }
        }
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
