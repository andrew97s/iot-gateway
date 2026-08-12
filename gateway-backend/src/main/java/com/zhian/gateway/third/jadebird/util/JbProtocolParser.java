package com.zhian.gateway.third.jadebird.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.exception.job.TaskException;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.TelemetryPayload;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.framework.cache.RedisCache;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.mapper.ZaAlarmTypeMapper;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.third.jadebird.vo.ElectricVo;
import com.zhian.gateway.third.jadebird.vo.Facility;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import com.zhian.gateway.third.jadebird.vo.StatInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * 青鸟数据解析工具
 *
 * @author tongwenjin
 * @since 2026/7/31
 */
@Slf4j
public class JbProtocolParser {

    private static String WIM_TYPE_CODES = "WIM,HRPWIM,LoraWIM,WIOM,HRPWIOM,4GWIOM";
    private static String WM_TYPE_CODES = "WA,HRPWA,LoraWA";
    private static String ELECTRIC_TYPE_CODES = "WCEFMD,NBWCEFMD,HRPWCEFMDM,LNEFMDM,LNCEFMD,,LNFMM,LNEMMT,LNSCM,LNSMD";

    public static List<Message> extractMessage(ZaSysDevice device, MonitorMsg msg) {
        List<Message> msgList = new ArrayList<>();
        TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);

        String event = msg.getEvent();
        // 解析告警消息
        if (StrUtil.equals(event, "alarm")) {
            List<StatInfo> stat = msg.getStat();
            for (StatInfo st : stat) {
                Optional<ZaAlarmType> type = typeMapping.resolveAlarmType("jb", st.getVal() + "");
                if (!type.isPresent()) {
                    log.info("提取青鸟告警事件类型失败:{}未注册告警类型!", st.getVal());
                    continue;
                }

                // TODO 针对阈值预警 ，告警描述可能需要修改
                String desc = "";

                // 新的设备告警
                msgList.add(MessageBuilder.buildAlarm(device, type.get(), desc, ""));
            }
        }

        // 解析设备监测数据消息
        if (StrUtil.equals(event, "business")) {
            msgList.add(extractBusinessMessage(device, msg));
        }

        return msgList;
    }

    private static Message extractBusinessMessage(ZaSysDevice device, MonitorMsg monitorMsg) {
        Message message = null;
        Facility mf = monitorMsg.getFacility();
        String deviceType = device.getType();
        // 用电设备
        if (mf.getAnalogValue() != null && StrUtil.isNotBlank(mf.getAnalogType()) && ELECTRIC_TYPE_CODES.contains(deviceType)) {
            return processElectricDevice(device, mf);
        }
        // 智能断路器
        else if (StrUtil.equals(deviceType, "ICB") && mf.getVoltageMain() != null) {
            return processICB(device, mf);
        }
        // HRP无线组合式电气火灾监控探测器
        else if (StrUtil.equals(deviceType, "HRPWCEFMD")) {
            return processElectric(device, mf);
        }
        //无线末端试水
        else if (StrUtil.equals(deviceType, "WTWTD")) {
            return processTerminalWater(device, mf);
        }
        // 部分设备的心跳数据只有模拟量，没有单位，需要先在监测配置中按型号添加监测项目，这里取第一个监测项目
        else if (mf.getAnalogValue() != null && StrUtil.isBlank(mf.getAnalogType())) {
            log.error("处理青鸟心跳数据时没有找到模拟量的单位，将忽略: {}", mf.getAddrStr());
        }
        // 青鸟V3A3 用电设备监测数据
        else if (StrUtil.containsIgnoreCase(mf.getFacilitiesModel(), "V3A3")) {
            return processV3A3(device, mf);
        }

        // 处理无线设备的空包
        if (CollUtil.isEmpty(monitorMsg.getStat())) {
            if (WIM_TYPE_CODES.contains(device.getType()) || WM_TYPE_CODES.contains(device.getType())) {
                // 无线输入输出模块的空包，做停止处理
                message = MessageBuilder.buildAlarm(device, null, "空包处理设备停止", null);
            }

            //恢复无线设备的故障状态
            if (StrUtil.equals(device.getOnline(), "0")) {
                log.info("无线设备{}空包数据已被同步为在线状态!", device.getCode());
                device.setOnline("1");
                SpringUtils.getBean(IZaSysDeviceService.class).updateZaSysDevice(device);
            }
        }

        // 更新设备状态
        if (StrUtil.isNotBlank(mf.getVoltage()) || StrUtil.isNotBlank(mf.getRssi())
                || StrUtil.isNotBlank(mf.getTemperature()) || mf.getExtraInfo() != null) {

            TelemetryPayload.Telemetry battery = MessageBuilder.builderTelemetry(
                    fetchMonitorType("battery"), mf.getVoltage(), "电池电量"
            );
            TelemetryPayload.Telemetry rssi = MessageBuilder.builderTelemetry(
                    fetchMonitorType("rssi"), mf.getVoltage(), "信号强度"
            );
            TelemetryPayload.Telemetry temperature = MessageBuilder.builderTelemetry(
                    fetchMonitorType("temperature"), mf.getVoltage(), "温度"
            );

            message = MessageBuilder.buildTelemetry(device, Arrays.asList(battery, rssi, temperature));

            // TODO 忽略 V3A3 断路器的非标监测数据

        }

        return message;
    }

    private static Message processElectricDevice(ZaSysDevice device, Facility mf) {
        ZaMonitorType type = fetchMonitorType(mf.getAnalogType());

        // 监测值描述
        String descr = mf.getDescr();
        String content = mf.getAnalogValue() + type.getUnit();
        if (descr.contains("A相") && descr.contains("温度")) {
            content = "A相温度 " + content;
        } else if (mf.getDescr().contains("B相") && mf.getDescr().contains("温度")) {
            content = "B相温度 " + content;
        } else if (mf.getDescr().contains("C相") && mf.getDescr().contains("温度")) {
            content = "C相温度 " + content;
        } else if (mf.getDescr().contains("零线温度")) {
            content = "零线温度 " + content;
        } else if (mf.getDescr().contains("剩余电流")) {
            content = "剩余电流 " + content;
        } else {
            content = mf.getChn() + "通道 " + mf.getAnalogValue() + type.getUnit();
        }

        // 电流
        TelemetryPayload.Telemetry telemetry = MessageBuilder.builderTelemetry(
                type, mf.getAnalogValue() + "", content
        );
        telemetry.setChannel(Integer.parseInt(mf.getChn()));

        return MessageBuilder.buildTelemetry(device, Collections.singletonList(telemetry));
    }

    private static Message processICB(ZaSysDevice device, Facility mf) {
        TelemetryPayload.Telemetry voltage = MessageBuilder.builderTelemetry(
                fetchMonitorType("voltage"), mf.getVoltageMain(), "断路器电压"
        );
        voltage.setChannel(1);

        // 电流
        TelemetryPayload.Telemetry current = MessageBuilder.builderTelemetry(
                fetchMonitorType("voltage"), mf.getCurrentMain(), "断路器电流"
        );
        current.setChannel(2);

        return MessageBuilder.buildTelemetry(
                device, Arrays.asList(voltage, current)
        );
    }

    private static Message processV3A3(ZaSysDevice device, Facility mf) {
        JSONObject mfObject = JSONObject.parseObject(JSONObject.toJSONString(mf));

        //电流
        String unit = "A";
        String[] items = {"currentA", "currentB", "currentC"};

        ZaMonitorType type = fetchMonitorType("current");

        List<TelemetryPayload.Telemetry> telemetries = new ArrayList<>();

        for (String item : items) {
            BigDecimal value = mfObject.getBigDecimal(item);
            if (value == null) {
                continue;
            }
            TelemetryPayload.Telemetry current = MessageBuilder.builderTelemetry(
                    type, value.toString(), item.substring(7) + "相电流" + value + unit
            );

            telemetries.add(current);
        }

        //温度
        unit = "℃";
        BigDecimal temp = mfObject.getBigDecimal("temperature");
        TelemetryPayload.Telemetry current = MessageBuilder.builderTelemetry(
                type, temp.toString(), "温度" + temp + unit
        );
        telemetries.add(current);

        return MessageBuilder.buildTelemetry(device, telemetries);
    }

    private static Message processTerminalWater(ZaSysDevice device, Facility mf) {
        // 手自动状态
        TelemetryPayload.Telemetry autoState = MessageBuilder.builderTelemetry(
                fetchMonitorType("manual_auto_mode"), mf.getManualAutoMode() != null ? mf.getManualAutoMode() + "" : "1",
                "手自动状态"
        );

        // 开关状态
        TelemetryPayload.Telemetry switchState = MessageBuilder.builderTelemetry(
                fetchMonitorType("switch"), mf.getSwitchMode() + "",
                "电磁阀开关状态"
        );

        // 开启时长(s)
        TelemetryPayload.Telemetry runTime = MessageBuilder.builderTelemetry(
                fetchMonitorType("time_second"), mf.getRunDuration() + "",
                "开启时长"
        );

        // 流量 & 高低阈值
        TelemetryPayload.Telemetry flow = MessageBuilder.builderTelemetry(
                fetchMonitorType("flow_ls"), mf.getFlow() + "",
                "流量"
        );
        flow.setThresholdLow(mf.getFlowThresholdLow().toString());
        flow.setThresholdHigh(mf.getFlowThresholdHigh().toString());

        // 压力 & 高低阈值
        TelemetryPayload.Telemetry pressure = MessageBuilder.builderTelemetry(
                fetchMonitorType("pressure_mpa"), mf.getPressure() + "",
                "压力"
        );
        pressure.setThresholdLow(mf.getPressureThresholdLow().toString());
        pressure.setThresholdHigh(mf.getPressureThresholdHigh().toString());

        return MessageBuilder.buildTelemetry(
                device, Arrays.asList(autoState, switchState, runTime, flow, pressure)
        );
    }

    private static ZaMonitorType fetchMonitorType(String code) {
        TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaMonitorType> type = typeMapping.resolveMonitorType("jb", code);
        if (!type.isPresent()) {
            throw new IllegalArgumentException("获取青鸟监测值类型失败:" + code + "未注册!");
        }

        return type.get();
    }

    private static Message processElectric(ZaSysDevice device, Facility mf) {
        Cache cache = SpringUtils.getBean(Cache.class);
        if (mf.getChannel() != null && mf.getChannelType() != null) {
            if (mf.getChannelType() != 2) {
                cache.setCacheMapValue("electric_channel", device.getId() + ":" + mf.getChannel(), mf.getChannelType());
            } else {
                cache.deleteCacheMapValue("electric_channel", device.getId() + ":" + mf.getChannel());
            }
        }

        List<TelemetryPayload.Telemetry> telemetries = new ArrayList<>();

        if (mf.getChannel() != null && mf.getAnalogValue() != null) {
            Integer channelType = cache.getCacheMapValue("electric_channel", device.getId() + ":" + mf.getChannel());
            if (channelType == null) {
                return null;
            }
            // 剩余电流
            else if (channelType == 0) {
                TelemetryPayload.Telemetry current = MessageBuilder.builderTelemetry(
                        fetchMonitorType("current_ma"), mf.getAnalogValue() + "",
                        "剩余电流 " + mf.getAnalogValue() + "mA"
                );
                current.setChannel(mf.getChannel());
                current.setThresholdHigh(mf.getThresholdHigh().toString());
                current.setThresholdLow(mf.getThresholdLow().toString());

                telemetries.add(current);
            }
            // 线温
            else if (channelType == 1) {
                TelemetryPayload.Telemetry temperature = MessageBuilder.builderTelemetry(
                        fetchMonitorType("temperature"), mf.getAnalogValue() + "",
                        "线温 " + mf.getAnalogValue() + "°C"
                );
                temperature.setChannel(mf.getChannel());
                temperature.setThresholdHigh(mf.getThresholdHigh().toString());
                temperature.setThresholdLow(mf.getThresholdLow().toString());

                telemetries.add(temperature);
            }
        }

        // 解析电流
        BigDecimal zero = new BigDecimal(0);
        if (ArrayUtil.isNotEmpty(mf.getCurrentArr())) {
            int i = 1;
            for (ElectricVo electricVo : mf.getCurrentArr()) {
                if (electricVo.getAnalogValue().equals(zero)) {
                    continue;
                }

                // 电流
                BigDecimal value = electricVo.getAnalogValue();
                TelemetryPayload.Telemetry current = MessageBuilder.builderTelemetry(
                        fetchMonitorType("current"), value + "",
                        electricVo.getPhase() + "相电流 " + value + "A"
                );
                current.setChannel(i++);
                telemetries.add(current);
            }
        }
        // 解析电压
        if (mf.getVoltageArr() != null && mf.getVoltageArr().length > 0) {
            int i = 1;
            for (ElectricVo electricVo : mf.getVoltageArr()) {
                if (electricVo.getAnalogValue().equals(zero)) {
                    continue;
                }
                // 电压
                BigDecimal value = electricVo.getAnalogValue();
                TelemetryPayload.Telemetry voltage = MessageBuilder.builderTelemetry(
                        fetchMonitorType("current"), value + "",
                        electricVo.getPhase() + "相电压 " + value + "V"
                );
                voltage.setChannel(i++);
                telemetries.add(voltage);
            }
        }

        return MessageBuilder.buildTelemetry(device, telemetries);
    }
}
