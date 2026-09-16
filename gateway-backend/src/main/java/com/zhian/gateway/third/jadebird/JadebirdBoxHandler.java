package com.zhian.gateway.third.jadebird;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.ip.IpUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.AlarmConstants;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.common.vo.BoxAlarmInfo;
import com.zhian.gateway.third.common.vo.BoxDeviceInfo;
import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 青鸟云盒对接
 */
@Component
@Slf4j
public class JadebirdBoxHandler extends BasePlatformHandler<BoxMsg> {
    public static final String PLATFORM_NAME = "jbox";
    public static final String PROTOCOL_NAME = "jbox";
    public static final String CACHE_MAP = "jbox";
    public static final Integer OFFLINE_HOURS = 1;
    private static ZaSysPlatform zaSysPlatform;
    private static Boolean running = false;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        JadebirdBoxHandler.zaSysPlatform = zaSysPlatform;
        log.info("等待青鸟云盒推送过来的数据");
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        log.info("将忽略青鸟云盒推送过来的数据");
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
//        ZaSysDevice dc = new ZaSysDevice();
//        dc.setOnline(DictValue.DEVICE_ONLINE);
//        dc.setPfCode(getPlatform());
//        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
//
//        for (ZaSysDevice zaSysDevice : list) {
//            Long last = cache.getCacheMapValue(CACHE_MAP, "last_" + zaSysDevice.getId());
//            //判断是否离线
//            if (last == null || last + OFFLINE_HOURS * 3600 * 1000 < System.currentTimeMillis()) {
//                log.info("云盒{}已离线，将推送离线告警，并将设备标识为离线状态", zaSysDevice.getCode());
//
//                zaSysDevice.setOnline(DictValue.DEVICE_OFFLINE);
//                deviceService.updateZaSysDevice(zaSysDevice);
//
//                // TODO 离线消息
//                // pushState(zaSysDevice, AlarmType.OFFLINE.getCode());
//            }
//        }
        return running;
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return "jbox";
    }


    /**
     * 反向控制
     *
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        log.info("青鸟云盒暂不支持反控");
        return R.error(400, "青鸟云盒暂不支持反控");
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param boxMsg
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(BoxMsg boxMsg) {
        if (boxMsg == null) {
            log.error("不支持消息类型： {}", boxMsg);
            MsgProcessContext.failed("接收消息为空!");
            return null;
        }

        ProcessInfo info = null;
        String msg = boxMsg.getMsg();
        //心跳
        switch (boxMsg.getType()) {
            case BoxMsg.TYPE_HEARTBEAT: {
                processHeart(msg);
                break;
            }
            case BoxMsg.TYPE_REGISTER: {
                BoxDeviceInfo deviceInfo = JSONObject.parseObject(msg, BoxDeviceInfo.class);
                // 同步云盒设备信息
                SyncDevice syncDevice = SyncDevice.builder()
                        .code(deviceInfo.getDeviceId())
                        .name("云盒" + deviceInfo.getIp())
                        .model(deviceInfo.getDeviceModel())
                        .typeCode(DeviceTypeEnum.WCB.getCode())
                        .pfCode(zaSysPlatform.getCode())
                        .wireless("0")
                        .ip(deviceInfo.getIp())
                        .remark(JSONObject.toJSONString(deviceInfo)).build();
                ZaSysDevice sysDevice = DeviceUtil.syncDevice(syncDevice);
                MessageUtil.setDevice(sysDevice);
                MsgProcessContext.getProcessInfo().setDevice(sysDevice);

                break;
            }
            case BoxMsg.TYPE_REGISTER_CAMERA:
                info = registerCameras(msg);
                break;
            case BoxMsg.TYPE_ALARM: {
                BoxAlarmInfo alarmInfo = JSONObject.parseObject(msg, BoxAlarmInfo.class);
                //兼容旧云盒的告警类型
                if (alarmInfo.getEventType() != null && alarmInfo.getAlgorithmId() == null) {
                    alarmInfo.setAlgorithmId(alarmInfo.getEventType() - 500);
                }
                ZaSysDevice sysDevice = deviceService.selectZaSysDeviceByCode(alarmInfo.getDeviceId(), null);
                if (sysDevice == null) {
                    String errMsg = String.format("云盒设备%s未注册!", alarmInfo.getDeviceId());
                    MsgProcessContext.failed(errMsg);
                    return null;
                }
                MsgProcessContext.getProcessInfo().setDevice(sysDevice);

                TypeMappingService mappingService = SpringUtils.getBean(TypeMappingService.class);
                String alarmCode = alarmInfo.getAlgorithmId()
                        + (alarmInfo.getEventState() == 0
                        ? AlarmConstants.ALARM_SUFFIX_ON
                        : AlarmConstants.ALARM_SUFFIX_OFF);
                Optional<ZaAlarmType> type = mappingService.resolveAlarmType(getPlatform(), alarmCode);
                if (!type.isPresent()) {
                    String errMsg = String.format("告警类型%s未注册!", alarmCode);
                    MsgProcessContext.failed(errMsg);

                    return null;
                }

                MsgProcessContext.addMsg(
                        MessageBuilder.buildAlarm(sysDevice, type.get(), "", alarmInfo.getPicUrl())
                );
                break;
            }
            default:
                log.error("不支持云盒消息类型: {}", boxMsg.getType());
        }

        return info;
    }

    /**
     * 心跳逻辑, 处理云盒设备同步
     *
     * @param msg msg
     */
    private ZaSysDevice processHeart(String msg) {
        BoxDeviceInfo deviceInfo = JSONObject.parseObject(msg, BoxDeviceInfo.class);
        // 同步云盒设备信息
        SyncDevice syncDevice = SyncDevice.builder()
                .code(deviceInfo.getDeviceId())
                .name("云盒" + deviceInfo.getIp())
                .model(deviceInfo.getDeviceModel())
                .typeCode(DeviceTypeEnum.WCB.getCode())
                .pfCode(zaSysPlatform.getCode())
                .wireless("0")
                .ip(deviceInfo.getIp()).build();
        ZaSysDevice sysDevice = DeviceUtil.syncDevice(syncDevice);
        MessageUtil.setDevice(sysDevice);
        MsgProcessContext.getProcessInfo().setDevice(sysDevice);
        return sysDevice;
    }


    /**
     * 注册云盒里的摄像机
     *
     * @param msg msg
     */
    private ProcessInfo registerCameras(String msg) {
        String ip = IpUtils.getIpAddr();
        ZaSysDevice boxInfo = null;

        ZaSysDevice dc = new ZaSysDevice();
        dc.setType(DeviceTypeEnum.WCB.getCode());
        dc.setIp(ip);
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        if (StringUtils.isEmpty(list)) {
            log.error("云盒 {} 未注册", ip);
            return null;
        }

        boxInfo = list.get(0);
        MessageUtil.setDevice(boxInfo);
        MsgProcessContext.getProcessInfo().setDevice(boxInfo);

        dc.setType(DeviceTypeEnum.CAMERA.getCode());
        Pattern pattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

        JSONArray cameraList = JSONArray.parseArray(msg);
        List<MqMessage> msgList = new ArrayList<>();
        for (int i = 0; i < cameraList.size(); i++) {
            JSONObject camera = cameraList.getJSONObject(i);
            String rtsp = camera.getString("rtspUrl");
            if (StringUtils.isEmpty(rtsp)) {
                log.error("云盒摄像机没有RTSP流，无法判断对应设备： {}", camera.getString("cameraName"));
                continue;
            }

            Matcher matcher = pattern.matcher(rtsp);
            if (!matcher.find()) {
                log.error("云盒摄像机 {}, 未找到IP: {}", camera.getString("cameraName"), rtsp);
                continue;
            }
            dc.setIp(matcher.group());
            List<ZaSysDevice> cameras = deviceService.selectZaSysDeviceList(dc);
            if (StringUtils.isEmpty(cameras)) {
                // 同步设备信息
                SyncDevice syncDevice = SyncDevice.builder()
                        .code("jbox_camera_"  + camera.getString("cameraName"))
                        .name(camera.getString("cameraName"))
                        .net("null")
                        .model("")
                        .typeCode("camera")
                        .pfCode(getPlatform())
                        .wireless("1")
                        .ip(dc.getIp())
                        .remark(JSON.toJSONString(camera))
                        .build();
                DeviceUtil.syncDevice(syncDevice);

                log.error("未找到云盒摄像机 {},{}", dc.getIp(), camera.getString("cameraName"));
                continue;
            }

            // TODO 青鸟云盒设备已淘汰 ，此处不处理云盒业务
        }

        return ProcessInfo.newDevice(boxInfo, msg, msgList);
    }
}
