package com.zhian.gateway.third.jadebird;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.consts.AlarmConstants;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.common.vo.DetectorAlarmInfo;
import com.zhian.gateway.third.common.vo.DetectorInfo;
import com.zhian.gateway.third.common.vo.DetectorListInfo;
import com.zhian.gateway.third.jadebird.vo.DetectorMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 青瞳对接
 */
@Component
@Slf4j
public class JadebirdDetectorHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "detector";
    public static final String PROTOCOL_NAME = "detector";
    public static final String CACHE_MAP = "detector";
    public static final Integer OFFLINE_HOURS = 1;
    private static ZaSysPlatform zaSysPlatform;
    private boolean running = false;

    @Override
    public DeviceSyncInfo  syncDeviceStatus()
    {
        ZaSysDevice dc = new ZaSysDevice();
        dc.setOnline(DictValue.DEVICE_ONLINE);
        dc.setType(DeviceTypeEnum.WGP.getCode());
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);

        for (ZaSysDevice zaSysDevice : list) {
            Long last = DeviceUtil.getCommTime(zaSysDevice.getId());
            //判断是否离线
            if (last == null || last + OFFLINE_HOURS * 3600 * 1000 < System.currentTimeMillis()) {
                log.info("青瞳{}已离线，将推送离线告警，并将设备标识为离线状态", zaSysDevice.getCode());
                // 推送离线消息
                MsgProcessContext.addMsg(
                        MessageBuilder.buildDeviceState(
                                zaSysDevice , "0" , "心跳超时"
                        )
                );
                //更新状态
                zaSysDevice.setOnline(DictValue.DEVICE_OFFLINE);
                deviceService.updateZaSysDevice(zaSysDevice);
            }
        }
        return DeviceSyncInfo.success(1);
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
     * 反向控制 TODO
     *
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        log.info("青瞳反控:{}", controlVo);
        return R.error(400, "青瞳暂不支持反控");
    }

    /**
     * 处理接收到的消息
     *
     * @param msgObj
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        log.info("青瞳消息: {}", JSON.toJSONString(msgObj));
        DetectorMsg detectorMsg = (msgObj instanceof DetectorMsg) ?
                ((DetectorMsg) msgObj) : JSONObject.parseObject(msgObj.toString(), DetectorMsg.class);
        if (detectorMsg == null || StringUtils.isEmpty(detectorMsg.getMsg())) {
            return null;
        }

        List<MqMessage> msgList = new ArrayList<>();

        String msgType = null;
        fetchControlDevice(detectorMsg);
        switch (detectorMsg.getType()) {
            case DetectorMsg.TYPE_REGISTER_DEVICE:
                // 直接注册主机信息
                break;
            case DetectorMsg.TYPE_REGISTER_DETECTOR_LIST:
                //青瞳下属探测器的注册信息
                processRegisterDetectorList(detectorMsg);
                break;
            case DetectorMsg.TYPE_ALARM:
                //青瞳上报的告警信息
                processAlarm(detectorMsg);
                break;
            case DetectorMsg.TYPE_CANCEL_ALARM:
                //青瞳上报的取消告警信息
                processCancelAlarm(detectorMsg);
                break;
            case DetectorMsg.TYPE_HEARTBEAT:
                // 心跳数据 - 判断主机在线、离线
                processHeart(detectorMsg);
                break;
            default:
                log.error("不支持青瞳消息类型: {}", detectorMsg.getType());
        }
        return null;
    }

    /**
     * 处置青瞳设备告警
     *
     * @param detectorMsg de
     */
    private void processRegisterDetectorList(DetectorMsg detectorMsg) {
        DetectorListInfo detectorListInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorListInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(detectorListInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳主机 {} 未注册", detectorListInfo.getDeviceId());
            return;
        }
        for (DetectorListInfo.Detector detector : detectorListInfo.getDetectors()) {
            //记录摄像头信息
            Map<String, String> attr = new HashMap<>();
            attr.put("ip", detector.getIp());
            for (DetectorListInfo.Camera camera : detector.getCameras()) {
                if (camera.getRtspUrl().contains("ir_stream")) {
                    attr.put("ir", camera.getFlvUrl());
                    attr.put("rtsp_ir", camera.getRtspUrl());
                } else {
                    attr.put("visi", camera.getFlvUrl());
                    attr.put("rtsp_vs", camera.getRtspUrl());
                }
            }
            // 同步设备信息
            SyncDevice syncDevice = SyncDevice.builder()
                    .code(detector.getSn())
                    .name(detector.getName())
                    .net(netDetector.getCode())
                    .model(detector.getModel())
                    .typeCode(DeviceTypeEnum.ICFD.getCode())
                    .pfCode(zaSysPlatform.getCode())
                    .wireless("0")
                    .ip(detector.getIp())
                    .remark(JSONObject.toJSONString(attr))
                    .build();
           DeviceUtil.syncDevice(syncDevice);
        }
    }

    /**
     * 处置青瞳告警撤销信息
     *
     * @param detectorMsg detectorMsg
     */
    private void processCancelAlarm(DetectorMsg detectorMsg) {
        DetectorAlarmInfo alarmInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorAlarmInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(alarmInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳主机 {} 未注册", alarmInfo.getDeviceId());
            return;
        }

        ZaSysDevice detector = deviceService.selectZaSysDeviceByCode(alarmInfo.getDetectorSn(), alarmInfo.getDeviceId());
        if (detector == null) {
            log.error("青瞳 {} 探测器 {} 未注册", alarmInfo.getDeviceId(), alarmInfo.getDetectorSn());
            return;
        }

        String ak = "alarm_" + detector.getCode();
        DetectorAlarmInfo old = cache.getCacheMapValue(CACHE_MAP, ak);
        if (old != null) {
            alarmInfo.setAlarmType(old.getAlarmType());
            alarmInfo.setAlarmTypeName(old.getAlarmTypeName());
        }
        else {
            log.error("告警撤销处理失败,获取撤销告警信息为空!");
            return;
        }

        String typeCode = alarmInfo.getAlarmType().toString();
        Optional<ZaAlarmType> type = typeMappingService.resolveAlarmType(
                getPlatform(), typeCode + AlarmConstants.ALARM_SUFFIX_OFF
        );
        if (!type.isPresent()) {
            String errMsg = String.format("处理青瞳告警失败,告警类型%s未注册!" , typeCode);
            log.error(errMsg);
            return;
        }

        MsgProcessContext.addMsg(
                MessageBuilder.buildAlarm(detector , type.get() , old.getAlarmTypeDesc() +" 撤销" , null)
        );

        //清除缓存
        cache.deleteCacheMapValue(CACHE_MAP, ak);
        MessageUtil.setDevice(detector);
    }

    /**
     * 处置青瞳告警信息
     *
     * @param detectorMsg de
     */
    private MqMessage processAlarm(DetectorMsg detectorMsg) {
        DetectorAlarmInfo alarmInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorAlarmInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(alarmInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳告警消息处理失败,主机 {} 未注册", alarmInfo.getDeviceId());
            return null;
        }

        ZaSysDevice detector = deviceService.selectZaSysDeviceByCode(
                alarmInfo.getDetectorSn(), alarmInfo.getDeviceId()
        );
        if (detector == null) {
            log.error("青瞳 {} 探测器 {} 未注册", alarmInfo.getDeviceId(), alarmInfo.getDetectorSn());
            return null;
        }

        String typeCode = alarmInfo.getAlarmType().toString();
        Optional<ZaAlarmType> type = typeMappingService.resolveAlarmType(
                getPlatform(), typeCode + AlarmConstants.ALARM_SUFFIX_ON
        );
        if (!type.isPresent()) {
            String errMsg = String.format("处理青瞳告警失败,告警类型%s未注册!" , typeCode);
            log.error(errMsg);
            return null;
        }

        String alarmPic = "";
        if (StrUtil.isNotBlank(alarmInfo.getPicture1())) {
            alarmPic += alarmInfo.getPicture1();
        }
        if (StrUtil.isNotBlank(alarmInfo.getPicture2())) {
            alarmPic += StrUtil.isNotBlank(alarmPic) ? "," : "";
            alarmPic += alarmInfo.getPicture2();
        }

        MsgProcessContext.addMsg(
                MessageBuilder.buildAlarm(detector , type.get() , alarmInfo.getAlarmTypeDesc() ,alarmPic)
        );

        //记录到缓存，反控时可以取消
        cache.setCacheMapValue(CACHE_MAP, "alarm_" + detector.getCode(), alarmInfo);
        MessageUtil.setDevice(detector);

        return null;
    }

    /**
     * 处置心跳数据
     *
     * @param detectorMsg
     */
    private void processHeart(DetectorMsg detectorMsg) {
        DetectorInfo detectorInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorInfo.class);
        ZaSysDevice zaSysDevice = MessageUtil.getDevice();
        if (zaSysDevice == null) {
            zaSysDevice = deviceService.selectZaSysDeviceByCode(detectorInfo.getDeviceId(), null);
        }
        if (zaSysDevice == null) {
            log.error("青瞳主机 {} 未注册", detectorInfo.getDeviceId());
            return;
        }
    }

    private ZaSysDevice fetchControlDevice(DetectorMsg detectorMsg) {
        DetectorInfo detectorInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorInfo.class);
        ZaSysDevice zaSysDevice = null;

        // 获取当前ServletContext 下对应request的IP
        // 此处可以自动将主机注册进系统
        if (StrUtil.isNotBlank(detectorInfo.getDeviceId())) {
            String ip = StrUtil.isNotBlank(detectorInfo.getIp()) ? detectorInfo.getIp() : detectorMsg.getIp();
            //青瞳主机的注册信息
            SyncDevice syncDevice = SyncDevice.builder()
                    .code(detectorInfo.getDeviceId())
                    .name("青瞳" + ip)
                    .typeCode(DeviceTypeEnum.WGP.getCode())
                    .pfCode(zaSysPlatform.getCode())
                    .wireless("0")
                    .ip(ip)
                    .remark(detectorMsg.getMsg())
                    .build();
            zaSysDevice = DeviceUtil.syncDevice(syncDevice , this);
        }
        MessageUtil.setDevice(zaSysDevice);

        return zaSysDevice;
    }

}
