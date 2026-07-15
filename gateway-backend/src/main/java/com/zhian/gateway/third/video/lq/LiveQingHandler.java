package com.zhian.gateway.third.video.lq;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.sign.Md5Utils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.video.VideoHelper;
import com.zhian.gateway.third.video.vo.LiveQingResult;
import com.zhian.gateway.third.video.vo.ZaFacility;
import com.zhian.gateway.third.video.vo.ZaGwatewayResult;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 视频网关对接
 */
@Component
@Slf4j
public class LiveQingHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "liveqing";
    public static final String PROTOCOL_NAME = "lq";
    public static final String CACHE_MAP = "lq:";
    private static ZaSysPlatform zaSysPlatform;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VideoHelper videoHelper;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        log.info("登录视频网关");
        LiveQingHandler.zaSysPlatform = zaSysPlatform;
        isAlive();
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        log.info("将忽略{}视频网关推送过来的数据", LiveQingConst.MODEL_NAME);
        running = false;
        return true;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        Long last = cache.getCacheObject(CACHE_MAP + "last");
        //最多一个小时同步一次
        if (last != null) {
            return DeviceSyncInfo.fail("操作失败,操作过于频繁!");
        }
        ZaFacility[] gatewayList = null;
        try {
            ZaGwatewayResult result = videoHelper.listGateway(zaSysPlatform, LiveQingConst.MODEL_NAME);
            if (result == null || !result.isSuccess() || result.getData() == null || result.getData().length == 0) {
                log.info("没有查询到已注册网关");
            } else {
                gatewayList = result.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关对接失败", e.getMessage(), PLATFORM_NAME);
        }
        cache.setCacheObject(CACHE_MAP + "last", System.currentTimeMillis(), 1, TimeUnit.HOURS);

        //没有网关
        if (ArrayUtil.isEmpty(gatewayList)) {
            return DeviceSyncInfo.fail("操作失败 , 网关设备为空 !");
        }

        //同步各网关的设备
        int syncCount = 0;
        for (ZaFacility gateway : gatewayList) {
            log.info("开始同步视频网关{}的摄像机", gateway.getPosition());
            ZaSysDevice netDevice = null;
            try {
                netDevice = sync(gateway);

                if (StringUtils.isEmpty(netDevice.getOnline()) || StrUtil.equals(netDevice.getOnline() , "0")) {
                    netDevice.setOnline("1");
                    deviceService.updateZaSysDevice(netDevice);
                    DeviceUtil.pushDevice(DeviceUpdReq.newOnlineReq(netDevice,"同步发现在线"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                zaSysErrorService.log(
                        ZaSysError.TYPE_API_ERROR,
                        "视频网关对接失败", e.getMessage(), PLATFORM_NAME + ":" + gateway.getPropIp()
                );
                // 在线改离线
                if (netDevice != null && StrUtil.equals(netDevice.getOnline(), "1")) {
                    netDevice.setOnline("0");
                    deviceService.updateZaSysDevice(netDevice);
                    DeviceUtil.pushDevice(DeviceUpdReq.newOfflineReq(netDevice,"同步异常发现离线"));
                }
            }
            syncCount++;
        }

        return DeviceSyncInfo.success(syncCount);
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

    /**
     * 同步网关设备及配置的摄像机
     *
     * @param gateway
     */
    private ZaSysDevice sync(ZaFacility gateway) {
        //先同步网关设备本身
        ZaSysDevice netDevice = videoHelper.syncGateway(gateway, PLATFORM_NAME);

        String token = getToken(netDevice);
        if (token == null) {
            return netDevice;
        }


        String api = "http://" + gateway.getPropIp() + ":" + gateway.getPropPort() + "/api/v1/getchannelsconfig";
        //获取摄像机列表
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", token);
        String str = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), String.class).getBody();
        LiveQingResult result = JSONObject.parseObject(str, LiveQingResult.class);
        if (result == null || result.getLiveQing() == null) {
            log.error("获取视频网关摄像机列表{} 失败", gateway.getPosition());
            return netDevice;
        }

        MqMessage.Facility facility = new MqMessage.Facility();
        JSONArray list = JSONArray.parseArray(result.getLiveQing().getBody().get(LiveQingConst.FIELD_CHANNELS));
        for (int i = 0; i < list.size(); i++) {
            JSONObject row = list.getJSONObject(i);
            //禁用的是未开启的通道，不关注
            if (!row.getString(LiveQingConst.FIELD_ENABLE).equalsIgnoreCase("1")) {
                continue;
            }

            String ip = row.getString(LiveQingConst.FIELD_IP);
            String code = StringUtils.isEmpty(ip) ? gateway.getFacilityCode() + "_" + row.getString(LiveQingConst.FIELD_CHANNEL) : ip.replaceAll("\\.", "");
            String name = gateway.getPosition() + "_" + (StringUtils.isEmpty(ip) ? row.getString(LiveQingConst.FIELD_CHANNEL) : ip);
            ZaSysDevice zaSysDevice = videoHelper.syncCamera(gateway, PLATFORM_NAME, code, name, ip, row.toJSONString());

            facility.setWireless(false);
            facility.setType(DeviceType.CAMERA);
            facility.setCode(zaSysDevice.getCode());
            facility.setName(zaSysDevice.getName());
            facility.setChn(row.getInteger(LiveQingConst.FIELD_CHANNEL));
            facility.setNet(gateway.getFacilityCode());

            DeviceUtil.pushDevice(DeviceUpdReq.newAddReq(zaSysDevice, row.toJSONString()));
        }
        return netDevice;
    }


    /**
     * 登录网关，获取令牌
     *
     * @param netDevice
     * @return
     */
    private String getToken(ZaSysDevice netDevice) {
        String token = null; // 不缓存token cache.getCacheObject(CACHE_MAP+netDevice.getCode());
        if (token != null) {
            return token;
        }

        JSONObject rs = JSONObject.parseObject(netDevice.getRemark());
        String api = "http://" + rs.getString("ip") + ":" + rs.getString("port") + "/api/v1/login?";
        api += "username=" + rs.getString("username") + "&password=" + Md5Utils.hash(rs.getString("password")) + "&url_token_only=true";
        String str = restTemplate.getForObject(api, String.class);
        LiveQingResult result = JSONObject.parseObject(str, LiveQingResult.class);
        if (result == null || result.getLiveQing() == null) {
            log.error("登录视频网关{} 失败", netDevice.getName());
            return null;
        }
        token = result.getLiveQing().getBody().get("URLToken");
        cache.setCacheObject(CACHE_MAP + netDevice.getCode(), token, Integer.parseInt(result.getLiveQing().getBody().get("TokenTimeout")) - 3600, TimeUnit.SECONDS);
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
    public AjaxResult doControl(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return AjaxResult.error("设备信息不存在");
        }

        ZaSysDevice netDevice = deviceService.selectZaSysDeviceByCode(camera.getNet(), null);
        if (netDevice == null) {
            return AjaxResult.error("未找到视频网关");
        }

        JSONObject ns = JSONObject.parseObject(netDevice.getRemark());
        JSONObject cs = JSONObject.parseObject(camera.getRemark());
        //流地址自动拼接
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.success("http://" + ns.getString("ip") + ":10800/flv/hls/stream_" + cs.getString("Channel") + ".flv");
        } else if (ControlVo.CMD_PLAY_BACK.equalsIgnoreCase(controlVo.getCommand())) {
            return AjaxResult.error("暂不支持回放");
        }

        //截图和云台控制
        String api = "http://" + ns.getString("ip") + ":" + ns.getString("port");
        try {
            if (ControlVo.CMD_SNAP.equalsIgnoreCase(controlVo.getCommand())) {
                String token = getToken(netDevice);
                api += "/api/v1/getchannelsnap?channel=" + cs.getString("Channel") + "&stime=now&format=jpeg&timeout=10";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", token);
                byte[] result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), byte[].class).getBody();
                return AjaxResult.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(result));
            } else if (ControlVo.CMD_PTZ.equalsIgnoreCase(controlVo.getCommand())) {
                String token = getToken(netDevice);
                api += "/api/v1/ptzcontrol?channel=" + cs.getString("Channel") + "&command=" + controlVo.getValue() + "&speed=5&_=" + System.currentTimeMillis();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("token", token);
                LiveQingResult result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), LiveQingResult.class).getBody();
                return AjaxResult.success();
            } else {
                return AjaxResult.error("不支持操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关反控失败", e.getMessage(), api);
            return AjaxResult.error("视频网关调用失败");
        }
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param msgObj
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("视频网关{}插件暂时停止", zaSysPlatform.getCode());
        } else {
            log.info("收到消息： {}", msgObj);
        }

        return null;
    }

}
