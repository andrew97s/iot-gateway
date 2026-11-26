package com.zhian.gateway.third.dahua;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.file.FileUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.dahua.vo.DahuaAlarmVo;
import com.zhian.gateway.third.dahua.vo.DahuaCameraVo;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 通过SDK对接大华设备，接受主动注册，订阅事件
 * ip  是对外的IP地址，拉流/截图使用，不能是127.0.0.1
 * port 是主动注册功能的监测端口，默认9500
 * {
 * "username":"通用账号"，
 * "password":"通用密码"，
 * "videoChannel":"视频通道,0是可见光，1是红外"，
 * "videoType":"视频码流,0是主码流，3从码流"，
 * "eventChannel":"事件通道,0是可见光，1是红外"，
 * "mappingPort":"0|1，摄像机映射了37777端口，强制订阅事件"
 * }
 */
@Component("dhsdkHandler")
@Slf4j
public class DhSdkHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "dhsdk";
    public static final String PROTOCOL_NAME = "dh";
    public static ZaSysPlatform zaSysPlatform;
    private static Boolean running = false;

    /**
     * 服务端口
     **/
    public static Integer wsPort;

    @Value("${zhian.server.port}")
    public void setWsPort(Integer wsPort) {
        this.wsPort = wsPort;
    }

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        log.info("启动大华SDK对接插件：{}", zaSysPlatform.getCode());
        DhSdkHandler.zaSysPlatform = zaSysPlatform;
        running = true;
        return running;
    }

    @Override
    public boolean stop() {
        log.info("停止大华{}的相关操作", zaSysPlatform.getCode());
        running = false;
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     *
     * @return
     */
    @Override
    public boolean isAlive() {
        return running;
    }


    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }


    /**
     * 反向控制
     *
     * @param controlVo 控制对象
     * @return R
     */
    @Override
    public R doControl(ControlVo controlVo) {
        return R.error("暂时不支持控制");
    }

    /**
     * 响应图片内容
     *
     * @param imageFile
     * @return
     * @throws IOException
     */
    private R responseImage(File imageFile) throws IOException {
        byte[] imgBuff = FileUtils.readFileToByteArray(imageFile);
        imageFile.delete();
        return R.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imgBuff));
    }

    /**
     * 处理接收到的消息,主要是发现新设备/设备注册/智能告警事件
     *
     * @param msgObj
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("大华{}插件暂时停止", zaSysPlatform.getCode());
            return null;
        }

        ProcessInfo processInfo = null;
        List<MqMessage> msgList = new ArrayList<>();
        if (msgObj instanceof DahuaCameraVo) {
            //摄像机搜索或者注册消息，如果registerOnly=1，只处理注册的摄像机，否则处理全部发现的摄像机
            DahuaCameraVo cameraVo = (DahuaCameraVo) msgObj;
            if (cameraVo.getRegister()) {
                String code = cameraVo.getRegId();
                ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(code, PLATFORM_NAME);
                if (zaSysDevice == null) {
                    zaSysDevice = new ZaSysDevice();
                    zaSysDevice.setOnline("1");
                    zaSysDevice.setNet(PLATFORM_NAME);
                    zaSysDevice.setCode(code);
                    zaSysDevice.setName(StringUtils.isEmpty(cameraVo.getName()) ? cameraVo.getCode() : cameraVo.getName());
                    zaSysDevice.setIp(cameraVo.getIp());
                    zaSysDevice.setWireless("0");
                    zaSysDevice.setType("camera");
                    zaSysDevice.setModel(cameraVo.getModel());
                    zaSysDevice.setPfCode(getPlatform());
                    zaSysDevice.setRemark(JSONObject.toJSONString(cameraVo));
                    deviceService.insertZaSysDevice(zaSysDevice);
                    log.info("添加新的大华摄像机{}-{}", code, cameraVo.getIp());
                    MqMessage.Facility facility = MqMessage.createFacility(zaSysDevice);
                    facility.setNet(null);//SDK接入暂时没有net字段
                    MqMessage mqMessage = MqMessage.createDevice(
                            zaSysDevice.getId(), PROTOCOL_NAME, facility, JSONObject.toJSONString(cameraVo)
                    );
                    msgList.add(mqMessage);
                }
                cameraVo.setDeviceId(zaSysDevice.getId());
                processInfo = ProcessInfo.newBusiness(zaSysDevice, JSON.toJSONString(cameraVo) , msgList);
            }
        } else if (msgObj instanceof DahuaAlarmVo) {
            DahuaAlarmVo alarmVo = (DahuaAlarmVo) msgObj;
            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(alarmVo.getCode(), PLATFORM_NAME);
            if (zaSysDevice == null) {
                log.error("大华摄像机{}未注册", alarmVo.getCode());
                return null;
            }

            MqMessage.Facility facility = MqMessage.createFacility(zaSysDevice);
            facility.setNet(null); //SDK接入暂时没有net字段
            MqMessage message = MqMessage.createAlarm(
                    zaSysDevice.getId(), 
                    PROTOCOL_NAME, facility, 
                    alarmVo.getAlarmType() + "", 
                    JSONObject.toJSONString(alarmVo)
            );
            msgList.add(message);
            processInfo = ProcessInfo.newAlarm(zaSysDevice, JSON.toJSONString(alarmVo) , msgList);
        } else {
            log.error("不支持的大华消息类型: {}", msgObj);
        }

        return processInfo;
    }
}
