package com.zhian.gateway.third.ovi.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.ovi.vo.BandDataV2;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能手环消息处理器(安全守护)
 *
 * @author tongwenjin
 * @since 2024/8/12
 */

@Component
public class BandHandlerV2 extends BasePlatformHandler<BandDataV2> {
    public static final String CACHE_MAP = "aqsh";

    public static final Integer OFFLINE_HOURS = 6;

    private static boolean RUNNING = false;

    @Override
    public String getPlatform() {
        return "aqsh";
    }

    @Override
    public String getProtocol() {
        return "aqsh";
    }

    @Override
    public AjaxResult doControl(ControlVo controlVo) {
        return AjaxResult.error("暂未实现反控功能!");
    }

    @Override
    public ProcessInfo doProcessMsg(BandDataV2 data) {
        // 获取消息类型
        String dataType = data.getDataType();
        String imei = data.getData() != null ?
                data.getData().getImei() : data.getAlarmData().getSerialNumber();
        // 同步&查询对应设备数据
        SyncDevice syncDevice = SyncDevice.builder()
                .code(imei)
                .name(imei)
                .typeCode(DeviceTypeEnum.BRACELET.getCode())
                // 此处设备型号固定写死
                .model(data.getData() != null ? data.getData().getModel() : "aqsh01")
                .pfCode(getPlatform())
                .wireless(Constants.YES).build();
        ZaSysDevice device = DeviceUtil.syncDevice(syncDevice,this);
        List<MqMessage> msgList = new ArrayList<>();

        //推送在线状态
        cache.setCacheMapValue(CACHE_MAP, "heart_" + device.getId(), System.currentTimeMillis());
        if (device.getOnline() == null || device.getOnline().equalsIgnoreCase(DictValue.DEVICE_OFFLINE)) {
            device.setOnline(DictValue.DEVICE_ONLINE);
            deviceService.updateZaSysDevice(device);
            msgList.add( DeviceUtil.genMessage(DeviceUpdReq.newOnlineReq(device, null)));
        }

        MqMessage mqMessage;
        ProcessInfo info = null;
        // 构造巡检业务消息
        if (!"alarm".equals(dataType)) {
            mqMessage = MqMessage.createBusiness(
                    device.getId(),
                    getProtocol(),
                    extractFacility(data, device),
                    data.getOriginalData()
            );
            msgList.add(mqMessage);
            info = ProcessInfo.newBusiness(device  , JSON.toJSONString(data) , msgList);
        } else {
            // 构造告警消息
            mqMessage = MqMessage.createAlarm(
                    device.getId(),
                    getProtocol(),
                    extractFacility(data, device),
                    data.getAlarmData().getNotificationType(),
                    data.getOriginalData()
            );
            msgList.add(mqMessage);
            info = ProcessInfo.newAlarm(device  , JSON.toJSONString(data) ,  msgList);
        }

        return info;
    }

    private MqMessage.Facility extractFacility(BandDataV2 bandData, ZaSysDevice device) {
        BandDataV2.Data data = bandData.getData();
        BandDataV2.AlarmData alarmData = bandData.getAlarmData();
        MqMessage.Facility facility = new MqMessage.Facility();
        // 设备名称和编码固定为IMEI编码
        String imei = bandData.getData() != null ?
                bandData.getData().getImei() : bandData.getAlarmData().getSerialNumber();
        facility.setCode(imei);
        facility.setName(imei);
        // 固定为无线设备
        facility.setWireless(true);
        // 此处收到消息及代表手环设备为正常在线状态
        facility.setOnLine(true);
        facility.setPfCode(getPlatform());
        facility.setModel("aqsh01");
        facility.setType(device.getType());
        // 设备基础监测数据
        String battery = data != null ? data.getBattery() : alarmData.getPower();
        String rssi = alarmData != null ? alarmData.getGSM() : null;
        if (StrUtil.isNotBlank(battery)) {
            facility.setVoltage(Integer.parseInt(battery));
        }
        if (StrUtil.isNotBlank(rssi)) {
            facility.setRssi(Integer.parseInt(rssi));
        }

        return facility;
    }
}
