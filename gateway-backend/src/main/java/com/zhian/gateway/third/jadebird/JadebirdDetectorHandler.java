package com.zhian.gateway.third.jadebird;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.consts.DictValue;
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
                DeviceUtil.pushDevice(DeviceUpdReq.newOfflineReq(zaSysDevice , "心跳超时"));

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
        if (!running) {
            log.warn("青瞳消息处理失败,插件已暂停!");
            return null;
        }
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
                msgList = processRegisterDetectorList(detectorMsg);
                msgType = MsgConstants.MSG_TYPE_DEVICE;
                break;
            case DetectorMsg.TYPE_ALARM:
                //青瞳上报的告警信息
                msgList.add(processAlarm(detectorMsg));
                msgType = MsgConstants.MSG_TYPE_ALARM;
                break;
            case DetectorMsg.TYPE_CANCEL_ALARM:
                //青瞳上报的取消告警信息
                msgList.add(processCancelAlarm(detectorMsg));
                msgType = MsgConstants.MSG_TYPE_ALARM;
                break;
            case DetectorMsg.TYPE_HEARTBEAT:
                // 心跳数据 - 判断主机在线、离线
                MqMessage message = processHeart(detectorMsg);
                if (message != null) {
                    msgList.add(message);
                    msgType = MsgConstants.MSG_TYPE_DEVICE;
                }
                break;
            default:
                log.error("不支持青瞳消息类型: {}", detectorMsg.getType());
        }

        return StrUtil.isBlank(msgType) ?
                null :
                ProcessInfo.newInstance(MessageUtil.getDevice(), JSON.toJSONString(detectorMsg), msgType, msgList);
    }

    /**
     * 处置青瞳设备告警
     *
     * @param detectorMsg
     */
    private List<MqMessage> processRegisterDetectorList(DetectorMsg detectorMsg) {
        DetectorListInfo detectorListInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorListInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(detectorListInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳主机 {} 未注册", detectorListInfo.getDeviceId());
            return null;
        }
        List<MqMessage> msgList = new ArrayList<>();
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
            ZaSysDevice zaSysDevice = DeviceUtil.syncDevice(syncDevice , this);

            //推送设备信息
            msgList.add(
                    DeviceUtil.genMessage(DeviceUpdReq.newAddReq(zaSysDevice,JSONObject.toJSONString(detector)))
            );
        }

        return msgList;
    }

    /**
     * 处置青瞳告警撤销信息
     *
     * @param detectorMsg
     */
    private MqMessage processCancelAlarm(DetectorMsg detectorMsg) {
        DetectorAlarmInfo detectorAlarmInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorAlarmInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(detectorAlarmInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳主机 {} 未注册", detectorAlarmInfo.getDeviceId());
            return null;
        }

        ZaSysDevice detector = deviceService.selectZaSysDeviceByCode(detectorAlarmInfo.getDetectorSn(), detectorAlarmInfo.getDeviceId());
        if (detector == null) {
            log.error("青瞳 {} 探测器 {} 未注册", detectorAlarmInfo.getDeviceId(), detectorAlarmInfo.getDetectorSn());
            return null;
        }

        MqMessage.Facility facility = new MqMessage.Facility();
        //推送告警信息
        facility.setWireless(false);
        facility.setType(detector.getType());
        facility.setCode(detector.getCode());
        facility.setName(detector.getName());
        facility.setNet(detector.getNet());
        facility.setModel(detector.getModel());

        MqMessage mqMessage = new MqMessage();
        mqMessage.setDeviceId(detector.getId());
        mqMessage.setEvent(MqMessage.EVENT_ALARM);
        mqMessage.setFacility(facility);
        mqMessage.setTime(new Date());
        mqMessage.setProtocol(PROTOCOL_NAME);
        mqMessage.setTime(detectorAlarmInfo.getTime());
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());

        String ak = "alarm_" + facility.getCode();
        DetectorAlarmInfo old = cache.getCacheMapValue(CACHE_MAP, ak);
        if (old != null) {
            detectorAlarmInfo.setAlarmType(old.getAlarmType());
            detectorAlarmInfo.setAlarmTypeName(old.getAlarmTypeName());
            mqMessage.setEventType(old.getAlarmType().toString());
            mqMessage.setMsgData(JSONObject.toJSONString(detectorAlarmInfo));
        }

        //清除缓存
        cache.deleteCacheMapValue(CACHE_MAP, ak);
        MessageUtil.setDevice(detector);

        return mqMessage;
    }

    /**
     * 处置青瞳告警信息
     *
     * @param detectorMsg
     */
    private MqMessage processAlarm(DetectorMsg detectorMsg) {
        DetectorAlarmInfo detectorAlarmInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorAlarmInfo.class);
        ZaSysDevice netDetector = MessageUtil.getDevice();
        if (netDetector == null) {
            netDetector = deviceService.selectZaSysDeviceByCode(detectorAlarmInfo.getDeviceId(), null);
        }
        if (netDetector == null) {
            log.error("青瞳告警消息处理失败,主机 {} 未注册", detectorAlarmInfo.getDeviceId());
            return null;
        }

        ZaSysDevice detector = deviceService.selectZaSysDeviceByCode(
                detectorAlarmInfo.getDetectorSn(), detectorAlarmInfo.getDeviceId()
        );
        if (detector == null) {
            log.error("青瞳 {} 探测器 {} 未注册", detectorAlarmInfo.getDeviceId(), detectorAlarmInfo.getDetectorSn());
            return null;
        }

        MqMessage.Facility facility = new MqMessage.Facility();
        //推送设备信息
        facility.setWireless(false);
        facility.setType(DeviceTypeEnum.ICFD.getCode());
        facility.setCode(detector.getCode());
        facility.setName(detector.getName());
        facility.setNet(detector.getNet());
        facility.setModel(detector.getModel());
        MqMessage mqMessage = MqMessage.createAlarm(
                detector.getId(), PROTOCOL_NAME, facility, detectorAlarmInfo.getAlarmType().toString(), detectorMsg.getMsg()
        );
        mqMessage.setTime(detectorAlarmInfo.getTime());

        //记录到缓存，反控时可以取消
        cache.setCacheMapValue(CACHE_MAP, "alarm_" + facility.getCode(), detectorAlarmInfo);
        MessageUtil.setDevice(detector);

        return mqMessage;
    }

    /**
     * 处置心跳数据
     *
     * @param detectorMsg
     */
    private MqMessage processHeart(DetectorMsg detectorMsg) {
        DetectorInfo detectorInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorInfo.class);
        ZaSysDevice zaSysDevice = MessageUtil.getDevice();
        if (zaSysDevice == null) {
            zaSysDevice = deviceService.selectZaSysDeviceByCode(detectorInfo.getDeviceId(), null);
        }
        if (zaSysDevice == null) {
            log.error("青瞳主机 {} 未注册", detectorInfo.getDeviceId());
            return null;
        }

        return null;
    }

    private ZaSysDevice fetchControlDevice(DetectorMsg detectorMsg) {
        DetectorInfo detectorInfo = JSONObject.parseObject(detectorMsg.getMsg(), DetectorInfo.class);
        ZaSysDevice zaSysDevice = null;

        // 获取当前ServletContext 下对应request的IP
        // 此处可以自动将主机注册进系统
        if (StrUtil.isNotBlank(detectorInfo.getDeviceId())) {
            String ip = StrUtil.isNotBlank(detectorInfo.getIp()) ? detectorInfo.getIp() : detectorMsg.getIp();
            //青瞳主机的注册信息
            MqMessage.Facility facility = new MqMessage.Facility();
            // 同步设备信息
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

            facility.setWireless(false);
            facility.setType("WGP");
            facility.setCode(detectorInfo.getDeviceId());
            facility.setName(zaSysDevice.getName());
        }
        MessageUtil.setDevice(zaSysDevice);

        return zaSysDevice;
    }

}
