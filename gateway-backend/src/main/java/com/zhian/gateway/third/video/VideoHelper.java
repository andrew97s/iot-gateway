package com.zhian.gateway.third.video;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.common.constants.AlarmType;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.jadebird.JadebirdInnerHandler;
import com.zhian.gateway.third.video.jinzhi.JinZhiHandler;
import com.zhian.gateway.third.video.vo.ZaFacility;
import com.zhian.gateway.third.video.vo.ZaGwatewayResult;
import com.zhian.gateway.third.vo.MqMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 视频网关信息获取接口
 */
@Component
public class VideoHelper {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IZaSysDeviceService deviceService;
    @Autowired
    private JadebirdInnerHandler jadebirdInnerHandler;


    /**
     * 获取已经配置的视频网关
     * @return
     */
    public ZaGwatewayResult listGateway(ZaSysPlatform zaSysPlatform, String modelName){
        String api = "http://"+zaSysPlatform.getIp()+":"+zaSysPlatform.getPort()+"/open/gateway/video?modelName="+modelName;
        return restTemplate.getForObject(api, ZaGwatewayResult.class);
    }

    /**
     * 同步网关设备
     * @param gateway
     * @param pfCode
     * @return
     */
    public ZaSysDevice syncGateway(ZaFacility gateway, String pfCode){
        JSONObject rs = new JSONObject();
        rs.put("ip", gateway.getPropIp());
        rs.put("port", gateway.getPropPort());
        rs.put("username", gateway.getPropUsername());
        rs.put("password", gateway.getPropPassword());

        ZaSysDevice dc = new ZaSysDevice();
        dc.setCode(gateway.getFacilityCode());
        dc.setType(DeviceType.VAG);
        List<ZaSysDevice> netList = deviceService.selectZaSysDeviceList(dc);
        if(netList == null || netList.isEmpty()){
            dc.setName(gateway.getPosition());
            dc.setModel(gateway.getModelName());
            dc.setPfCode(pfCode);
            dc.setIp(gateway.getPropIp());

            dc.setRemark(rs.toJSONString());
            deviceService.insertZaSysDevice(dc);
        }else {
            dc = netList.get(0);
            dc.setIp(gateway.getPropIp());
            dc.setRemark(rs.toJSONString());
            dc.setName(gateway.getPosition());
            deviceService.updateZaSysDevice(dc);
        }
        return dc;
    }

    /**
     * 同步摄像机信息
     * @param gateway
     * @param pfCode
     * @param code
     * @param name
     * @param ip
     * @param remark
     * @return
     */
    public ZaSysDevice syncCamera(ZaFacility gateway, String pfCode, String code, String name, String ip, String remark){
        ZaSysDevice dc = new ZaSysDevice();
        dc.setCode(code);
        dc.setType(DeviceType.CAMERA);
        dc.setNet(gateway.getFacilityCode());
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        if(list == null || list.isEmpty()){
            dc.setName(name);
            dc.setPfCode(pfCode);
            dc.setIp(ip);
            dc.setRemark(remark);
            deviceService.insertZaSysDevice(dc);
        }else {
            dc = list.get(0);
            boolean updated = false;
            if(ip != null && (dc.getIp() == null || !dc.getIp().equalsIgnoreCase(ip))){
                dc.setIp(ip);
                updated = true;
            }
            if(remark != null && (dc.getRemark() == null || !dc.getRemark().equalsIgnoreCase(remark))) {
                dc.setRemark(remark);
                dc.setName(name);
                updated = true;
            }
            if(updated) {
                deviceService.updateZaSysDevice(dc);
            }
        }
        return dc;
    }

    /**
     * 推送网关/摄像机的设备状态
     * @param zaSysDevice
     * @param alarm
     */
    public void pushState(ZaSysDevice zaSysDevice, AlarmType alarm){
        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setWireless(zaSysDevice.getWireless() != null && "Y".equalsIgnoreCase(zaSysDevice.getWireless()));
        facility.setType(zaSysDevice.getType());
        facility.setCode(zaSysDevice.getCode());
        facility.setName(zaSysDevice.getName());
        facility.setModel(zaSysDevice.getModel());
        facility.setNet(zaSysDevice.getNet());
        facility.setOnLine(Constants.YES.equals(zaSysDevice.getOnline()));
        jadebirdInnerHandler.consumeMsg(MqMessage.createAlarm(zaSysDevice.getId(), JinZhiHandler.PROTOCOL_NAME, facility, alarm.getCode(), JSONObject.toJSONString(alarm)));
    }
}
