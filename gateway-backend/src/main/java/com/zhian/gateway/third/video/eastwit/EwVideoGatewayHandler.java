package com.zhian.gateway.third.video.eastwit;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.file.FileUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.utils.VideoUtil;
import com.zhian.gateway.third.video.eastwit.vo.GatewayVideoInfo;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 东智视频网关相关服务实现类
 *
 * @author tongwenjin
 * @since 2022 /11/9
 */
@Component
@Slf4j
public class EwVideoGatewayHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "eastwit";
    public static final String PROTOCOL_NAME = "ew";
    private static Boolean running = false;
    private static Map<String, ZaSysDevice> gatewayMap = new ConcurrentHashMap<>();
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        for (String id : gatewayMap.keySet()) {
            Long ts = cache.getCacheMapValue(PLATFORM_NAME, id);
            if (ts == null || ts + 3600 * 1000 < System.currentTimeMillis()) {
                ZaSysDevice gateway = gatewayMap.get(id);
                gateway.setOnline("0");
                deviceService.updateZaSysDevice(gateway);
                //  推送网关离线消息
                DeviceUtil.pushDevice(DeviceUpdReq.newOfflineReq(gateway , "心跳超时"));
            }
        }
        return DeviceSyncInfo.success(gatewayMap.size());
    }

    /**
     *
     * @param msgObject msgObject
     */
    @Override
    @SuppressWarnings("unchecked")
    public ProcessInfo doProcessMsg(Object msgObject) {
        if (Objects.isNull(msgObject)) {
            return null;
        }
        List<MqMessage> msgList = new ArrayList<>();
        GatewayVideoInfo videoInfo = JSONObject.parseObject(msgObject.toString(), GatewayVideoInfo.class);
        // 添加视频网关设备
        GatewayVideoInfo.GatewayInfo gatewayInfo = videoInfo.getGatewayInfo();
        ZaSysDevice gateway = gatewayMap.get(gatewayInfo.getGatewayId());
        if (gateway == null) {
            gateway = new ZaSysDevice();
            gateway.setModel(PLATFORM_NAME);
            gateway.setType(DeviceType.VAG);
            gateway.setName(gatewayInfo.getGatewayName());
            gateway.setCode(gatewayInfo.getGatewayId());
            gateway.setBizId(gatewayInfo.getGatewayId());
            gateway.setIp(gatewayInfo.getGatewayIp());
            gateway.setOnline("1");
            gateway.setWireless("0");
            gateway.setPfCode(PLATFORM_NAME);
            deviceService.insertZaSysDevice(gateway);
            gatewayMap.put(gatewayInfo.getGatewayId(), gateway);
            msgList.add(DeviceUtil.genMessage(DeviceUpdReq.newAddReq(gateway, null)));
        }
        // 网关在线
        else if (gateway.getOnline().equalsIgnoreCase("0")) {
            gateway.setOnline("1");
            deviceService.updateZaSysDevice(gateway);
            msgList.add(DeviceUtil.genMessage(DeviceUpdReq.newOnlineReq(gateway, null)));
        }

        ZaSysDevice dc = new ZaSysDevice();
        dc.setNet(gateway.getCode());
        dc.setType(DeviceType.CAMERA);
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        Map<String, ZaSysDevice> oldMap = list.stream().collect(Collectors.toMap(ZaSysDevice::getCode, ZaSysDevice -> ZaSysDevice));
        for (GatewayVideoInfo.DeviceInfo dev : videoInfo.getDevlist()) {
            ZaSysDevice camera = dev.toCamera();
            oldMap.remove(camera.getCode());
            ZaSysDevice old = deviceService.selectZaSysDeviceByCode(camera.getCode(), gateway.getCode());
            // 设备新增
            if (old == null) {
                camera.setNet(gateway.getCode());
                camera.setPfCode(PLATFORM_NAME);
                deviceService.insertZaSysDevice(camera);
                msgList.add(DeviceUtil.genMessage(DeviceUpdReq.newAddReq(camera, null)));
            }
            // 设备在线、离线状态
            else if (!old.getOnline().equalsIgnoreCase(camera.getOnline())) {
                old.setOnline(camera.getOnline());
                deviceService.updateZaSysDevice(old);
                boolean offline = StrUtil.equals(camera.getOnline(), "0");
                msgList.add(
                        DeviceUtil.genMessage(
                                offline ?
                                        DeviceUpdReq.newOfflineReq(old, null) :
                                        DeviceUpdReq.newOnlineReq(old, null)
                        )
                );
            } else if (!old.getRemark().equalsIgnoreCase(camera.getRemark())) {
                old.setRemark(camera.getRemark());
                deviceService.updateZaSysDevice(old);
            }
        }
        if (!oldMap.isEmpty()) {
            for (ZaSysDevice camera : oldMap.values()) {
                deviceService.deleteZaSysDeviceById(camera.getId());
                msgList.add(DeviceUtil.genMessage(DeviceUpdReq.newDelReq(camera, null)));
            }
        }
        cache.setCacheMapValue(PLATFORM_NAME, gatewayInfo.getGatewayId(), System.currentTimeMillis());

        return ProcessInfo.newDevice(gateway, JSON.toJSONString(videoInfo), msgList);
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public AjaxResult doControl(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return AjaxResult.error("设备信息不存在");
        }

        ZaSysDevice netDevice = deviceService.selectZaSysDeviceByCode(camera.getNet(), null);
        if (netDevice == null) {
            return AjaxResult.error("未找到视频网关");
        }
        GatewayVideoInfo.DeviceInfo.PlayUrl playUrl = JSONObject.parseObject(camera.getRemark(), GatewayVideoInfo.DeviceInfo.PlayUrl.class);
        //流地址
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.success(playUrl.getFlv());
        } else if (ControlVo.CMD_PLAY_BACK.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.error("暂不支持回放");
        } else if (ControlVo.CMD_RECORDS.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.error("暂不支持回放");
        } else if (ControlVo.CMD_PTZ.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.error("不支持云台控制");
        } else if (ControlVo.CMD_SNAP.equalsIgnoreCase(controlVo.getCommand())) {
            //截图自己实现
            try {
                String rtsp = playUrl.getRtsp();
                if (StringUtils.isEmpty(rtsp)) {
                    log.error("截图 {} - {} 时未找到视频流", camera.getNet(), camera.getCode());
                    return AjaxResult.error("未找到视频流");
                }

                String path = VideoUtil.ffmpeg(rtsp);
                if (StringUtils.isEmpty(path)) {
                    log.error("FFMPEG截图 {} - {} 失败", camera.getNet(), camera.getCode());
                    return AjaxResult.error("截图失败");
                }

                File imgFile = new File(ZhianConfig.getUploadPath() + path);
                if (!imgFile.exists()) {
                    log.error("未找到FFMPEG截图文件 {} - {} : {}", camera.getNet(), camera.getCode(), path);
                    return AjaxResult.error("截图文件不存在");
                }
                byte[] result = FileUtils.readFileToByteArray(imgFile);
                imgFile.delete();
                return AjaxResult.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(result));


            } catch (Exception e) {
                e.printStackTrace();
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关反控失败", e.getMessage(), JSONObject.toJSONString(controlVo));
                return AjaxResult.error("视频网关调用失败");
            }
        }
        return AjaxResult.error("不支持操作");
    }
}
