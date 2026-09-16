package com.zhian.gateway.third.video.lt;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.video.VideoHelper;
import com.zhian.gateway.third.video.vo.ZaFacility;
import com.zhian.gateway.third.video.vo.ZaGwatewayResult;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 视频网关对接 TODO
 */
@Component
@Slf4j
public class LtsrvHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "ltsrv";
    public static final String PROTOCOL_NAME = "lt";
    public static final String CACHE_MAP = "lt:";
    private static Boolean running = false;

    @Autowired
    private VideoHelper videoHelper;

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        Long last = cache.getCacheObject(CACHE_MAP + "last");
        if (last != null) {
            return DeviceSyncInfo.success(1);
        }
        int syncCount = 0;
        //一个小时同步一次
        try {
            ZaGwatewayResult result = videoHelper.listGateway(platform, "LTSRV");
            if (result == null || !result.isSuccess() || result.getData() == null || result.getData().length == 0) {
                log.info("没有查询到已注册网关");
                return DeviceSyncInfo.fail("没有查询到已注册网关");
            } else {
                ZaFacility[] data = result.getData();

                for (ZaFacility gateway : result.getData()) {
                    log.info("开始同步视频网关{}的摄像机", gateway.getPosition());
                    sync(gateway);
                    syncCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关对接失败", e.getMessage(), PLATFORM_NAME);
        }
        cache.setCacheObject(CACHE_MAP + "last", System.currentTimeMillis(), 1, TimeUnit.HOURS);

        return DeviceSyncInfo.success(1);
    }

    /**
     * 同步网关设备及配置的摄像机
     *
     * @param gateway
     */
    private void sync(ZaFacility gateway) {
        //先同步网关设备本身
        ZaSysDevice netDevice = videoHelper.syncGateway(gateway, PLATFORM_NAME);

        //令牌
        String token = getToken(netDevice);
        if (token == null) {
            return;
        }

        //获取摄像机列表
        String api = "http://" + gateway.getPropIp() + ":" + gateway.getPropPort() + "/GetDeviceList";
        String json = DigestRequest.post(api, gateway.getPropUsername(), gateway.getPropPassword(), "{}", token);
        if (StringUtils.isEmpty(json)) {
            return;
        }
        JSONObject ret = JSONObject.parseObject(json);
        if (!ret.containsKey("DeviceList")) {
            log.error("错误的视频网关返回信息: {}", ret.toJSONString());
            return;
        }

        MqMessage.Facility facility = new MqMessage.Facility();
        JSONArray list = ret.getJSONArray("DeviceList");
        for (int i = 0; i < list.size(); i++) {
            JSONObject row = list.getJSONObject(i);
            ZaSysDevice zaSysDevice = videoHelper.syncCamera(gateway, PLATFORM_NAME, row.getString("id"), row.getString("name"), row.getString("ip"), row.toJSONString());

            facility.setWireless(false);
            facility.setType("camera");
            facility.setCode(zaSysDevice.getCode());
            facility.setName(zaSysDevice.getName());
            facility.setChn(row.getInteger("Channel"));
            facility.setNet(gateway.getFacilityCode());

            consumeMsg(MqMessage.createDevice(zaSysDevice.getId(), PROTOCOL_NAME, facility, row.toJSONString()));
        }

    }


    /**
     * 登录网关，获取令牌
     *
     * @param netDevice
     * @return
     */
    private String getToken(ZaSysDevice netDevice) {
        String token = cache.getCacheObject(CACHE_MAP + netDevice.getCode());
        if (token != null) {
            return token;
        }

        JSONObject rs = JSONObject.parseObject(netDevice.getRemark());
        String api = "http://" + rs.getString("ip") + ":" + rs.getString("port") + "/SysUserInf/Login";

        token = DigestRequest.login(api, rs.getString("username"), rs.getString("password"));
        if (token == null) {
            return null;
        }

        cache.setCacheObject(CACHE_MAP + netDevice.getCode(), token, 1, TimeUnit.HOURS);
        return token;
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
     * 反向控制,支持云台控制和拉视频流
     *
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return R.error("设备信息不存在");
        }

        ZaSysDevice netDevice = deviceService.selectZaSysDeviceByCode(camera.getNet(), null);
        if (netDevice == null) {
            return R.error("未找到视频网关");
        }

        JSONObject rs = JSONObject.parseObject(netDevice.getRemark());
        //流地址自动拼接
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            return R.success("wss://" + rs.getString("ip") + "/wstream/" + camera.getCode());
        }

        // 只支持截图和PTZ
        if (!ControlVo.CMD_SNAP.equals(controlVo.getCommand()) && !ControlVo.CMD_PTZ.equals(controlVo.getCommand())) {
            return R.error("不支持的反控操作");
        }

        String api = "http://" + rs.getString("ip") + ":" + rs.getString("port");
        try {
            //先取令牌，不存在或过期就重新登录
            String token = getToken(netDevice);
            //截图
            if (ControlVo.CMD_SNAP.equals(controlVo.getCommand())) {
                String json = DigestRequest.post(api + "/snap/" + camera.getCode(), rs.getString("username"), rs.getString("password"), "{}", token);
                if (StringUtils.isEmpty(json)) {
                    log.debug("视频网关拍照失败: {}", api);
                    return R.error("视频网关拍照失败");
                }
                JSONObject ret = JSONObject.parseObject(json);
                if (!ret.containsKey("uri")) {
                    log.error("错误的视频网关返回信息: {}", ret.toJSONString());
                    return R.error("视频网关拍照失败");
                }

                return R.success(ret.getString("uri"));
            } else if (ControlVo.CMD_PTZ.equals(controlVo.getCommand())) {
                //云台控制
                String json = DigestRequest.post(api + "/SysDeviceManage/ptz", rs.getString("username"), rs.getString("password"), "DevAcct=" + camera.getCode() + "&action=" + controlVo.getValue(), token);
                log.debug("视频网关 {}/SysDeviceManage/ptz, response: {}", api, json);
            }
            return R.success();
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关反控失败", e.getMessage(), api);
            return R.error("视频网关调用失败");
        }
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param msgObj
     * @return
     */
    @Override
    public void processMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("视频网关{}插件暂时停止", getPlatform());
        } else {
            log.info("收到消息： {}", msgObj);
        }
    }

}
