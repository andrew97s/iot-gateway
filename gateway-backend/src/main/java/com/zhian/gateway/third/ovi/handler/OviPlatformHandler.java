package com.zhian.gateway.third.ovi.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.ovi.vo.OviData;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 欧孚平台对接消息处理器
 *
 * @author tongwenjin
 * @since 2024/8/6
 */

@Component
@Slf4j
public class OviPlatformHandler extends BasePlatformHandler<Map<String,String>> {

    public static final String CACHE_MAP = "ovi";
    public static final Integer OFFLINE_HOURS = 6;
    private static boolean RUNNING = false;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        return RUNNING = true;
    }

    @Override
    public boolean stop() {
        return RUNNING = false;
    }

    @Override
    public boolean isAlive() {
//        ZaSysDevice dc = new ZaSysDevice();
//        dc.setType(DeviceTypeEnum.BRACELET.getCode());
//        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
//
//        for(ZaSysDevice zaSysDevice:list) {
//            Long last = cache.getCacheMapValue(CACHE_MAP, "heart_"+zaSysDevice.getId());
//            //判断是否离线
//            if (zaSysDevice.getOnline().equalsIgnoreCase(DictValue.DEVICE_ONLINE)
//                    && (last == null || last + OFFLINE_HOURS * 3600 * 1000 < System.currentTimeMillis())) {
//
//                zaSysDevice.setOnline(DictValue.DEVICE_OFFLINE);
//                deviceService.updateZaSysDevice(zaSysDevice);
//
//                // TODO 离线消息
//                // pushState(zaSysDevice, AlarmType.OFFLINE.getCode());
//            }
//        }
        return RUNNING;
    }

    @Override
    public String getPlatform() {
        return "ovi";
    }

    @Override
    public String getProtocol() {
        return "ovi";
    }

    @Override
    public R doControl(ControlVo controlVo) {
        return R.error("欧孚暂不支持设备反控");
    }

    @Override
    public ProcessInfo doProcessMsg(Map<String,String> request) {
        // 解析request
        OviData oviData = OviData.newInstance(request);
        // 同步&查询对应设备数据
        assert oviData != null;
        SyncDevice syncDevice = SyncDevice.builder()
                .code(oviData.getCode())
                .name(oviData.getCode())
                .typeCode(DeviceTypeEnum.BRACELET.getCode())
                // 此出设备型号固定写死
                .model("B2315G_QingNiaoZhiAn")
                .pfCode(getPlatform())
                .wireless(Constants.YES).build();
        ZaSysDevice device = DeviceUtil.syncDevice(syncDevice , this);
        List<MqMessage> msgList = new ArrayList<>();

        //推送在线状态
        cache.setCacheMapValue(CACHE_MAP, "heart_"+device.getId(), System.currentTimeMillis());
        if(device.getOnline() == null || device.getOnline().equalsIgnoreCase(DictValue.DEVICE_OFFLINE)) {
            device.setOnline(DictValue.DEVICE_ONLINE);
            deviceService.updateZaSysDevice(device);
            msgList.add(DeviceUtil.genMessage(DeviceUpdReq.newOnlineReq(device , null)));
        }


        //4代表是计步数据,6是心率数据,11是翻转数据,12代表是温度,14代表是双温度，10代表是血糖，8代表是血压，31代表是血氧，type=58代表睡眠数据，30代表是电池电量数据，16代表是GPS数据
        String type = request.get("type");
        Set<String> businessSet = Stream.of("4", "6", "11", "12", "14", "10", "8", "31", "58", "30", "16", "5").collect(Collectors.toSet());
        MqMessage mqMessage;
        // 构造巡检业务消息
        String msgType = MsgConstants.MSG_TYPE_ALARM;
        if(businessSet.contains(type)) {
            mqMessage = MqMessage.createBusiness(
                    device.getId(),
                    "ovi",
                    extractFacility(device, request),
                    JSON.toJSONString(request)
            );
            msgList.add(mqMessage);
            msgType = MsgConstants.MSG_TYPE_BUSINESS;
        }else{
            // 构造告警消息
            mqMessage = MqMessage.createAlarm(device.getId(), getProtocol(), extractFacility(device, request), type, JSON.toJSONString(request));
            msgList.add(mqMessage);
        }

        return ProcessInfo.newInstance(device, JSON.toJSONString(request) , msgType , msgList);
    }

    private MqMessage.Facility extractFacility(ZaSysDevice device , Map<String,String> request) {
        MqMessage.Facility facility = new MqMessage.Facility();
        // 设备名称和编码固定为IMEI编码
        facility.setCode(device.getCode());
        facility.setName(device.getName());
        // 固定为无线设备
        facility.setWireless(true);
        // 此处收到消息及代表手环设备为正常在线状态
        facility.setOnLine(true);
        facility.setPfCode(getPlatform());
        facility.setModel(device.getModel());
        facility.setType(device.getType());
        // 设备基础监测数据
        String signal = request.get("signal");
        String battery = request.get("battery");
        if (StrUtil.isNotBlank(signal)) {
            facility.setRssi(Integer.parseInt(signal));
        }
        if (StrUtil.isNotBlank(battery)) {
            facility.setRssi(Integer.parseInt(battery));
        }

        return facility;
    }
}
