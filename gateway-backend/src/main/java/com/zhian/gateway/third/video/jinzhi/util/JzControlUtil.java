package com.zhian.gateway.third.video.jinzhi.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.common.utils.sign.Md5Utils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.file.TempFileStore;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.video.jinzhi.JinZhiHandler;
import com.zhian.gateway.third.video.jinzhi.JinzhiConst;
import com.zhian.gateway.third.video.jinzhi.JinzhiWebSocket;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiRecord;
import com.zhian.gateway.third.video.vo.VideoRecord;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.websocket.Session;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 金智视频网关反控相关工具类
 *
 * @author tongwenjin
 * @since 2026/8/21
 */
@Slf4j
public class JzControlUtil {

    private static IZaSysDeviceService deviceService;
    private static ZaSysPlatform platform;
    private static RestTemplate restTemplate;
    private static Cache cache;
    // 延迟任务，主要是云台控制使用
    static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void init(ZaSysPlatform platform) {
        JzControlUtil.platform = platform;
        JzControlUtil.cache = SpringUtils.getBean(Cache.class);

        deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
        restTemplate = SpringUtils.getBean(RestTemplate.class);
    }

    public static R control(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return R.error("设备信息不存在");
        }

        ZaSysDevice netDevice = deviceService.selectZaSysDeviceByCode(camera.getNet(), null);
        if (netDevice == null) {
            return R.error("未找到视频网关");
        }

        try {
            switch (controlVo.getCommand()) {
                case ControlVo.CMD_STREAM:
                    return handleStream(netDevice, camera, controlVo);
                case ControlVo.CMD_PLAY_BACK:
                    return handlePlayback(netDevice, camera, controlVo);
                case ControlVo.CMD_RECORDS:
                    return handleRecords(netDevice, camera, controlVo);
                case ControlVo.CMD_SNAP:
                    return handleSnap(netDevice, camera, controlVo);
                case ControlVo.CMD_PTZ:
                    return handlePtz(netDevice, camera, controlVo);
                case "DHIPCTalkStartReq":
                    return handleDhStart(netDevice, camera, controlVo);
                case "DHIPCTalkStopReq":
                    return handleDhStop(netDevice, camera, controlVo);
                default:
                    return R.error("不支持的操作");
            }
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private static R handleStream(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        JSONObject cs = JSONObject.parseObject(camera.getRemark());
        Boolean useSsl = Boolean.parseBoolean(platform.getConfigStr("useSsl"));
        String protocol = StrUtil.isNotBlank(controlVo.getValue()) ? "ws" : controlVo.getValue();
        if (cs.containsKey("channelCount") && cs.getInteger("channelCount") > 1) {
            return R.success(
                    playUrlPrefix(useSsl, netDevice, protocol) + camera.getCode() + "-0.live.flv" + "," +
                            playUrlPrefix(useSsl, netDevice, protocol) + camera.getCode() + "-1.live.flv");
        } else {
            return R.success(playUrlPrefix(useSsl, netDevice, protocol) + camera.getCode() + ".live.flv");
        }
    }

    private static R handlePlayback(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        JSONObject cs = JSONObject.parseObject(camera.getRemark());
        Boolean useSsl = Boolean.parseBoolean(platform.getConfigStr("useSsl"));

        if (cs.containsKey("type") && JinzhiConst.TYPE_GB.equalsIgnoreCase(cs.getString("type"))) {
            //国标，回放参数是起止时间段
            return R.success(

                    playUrlPrefix(useSsl, netDevice, "http") +
                            camera.getCode() + "_" + controlVo.getValue() + ".live.flv"
            );
        } else if (cs.containsKey("type") && JinzhiConst.TYPE_DHSDKS.equalsIgnoreCase(cs.getString("type"))) {
            //回放参数是起止时间段
            if (cs.containsKey("channelCount") && cs.getInteger("channelCount") > 1) {
                return R.success(playUrlPrefix(useSsl, netDevice, "http") + camera.getCode() + "-0_" + controlVo.getValue() + ".live.flv," + playUrlPrefix(useSsl, netDevice, "http") + camera.getCode() + "-1_" + controlVo.getValue() + ".live.flv");
            } else {
                return R.success(playUrlPrefix(useSsl, netDevice, "http") + camera.getCode() + "_" + controlVo.getValue() + ".live.flv");
            }
        } else {
            //直连不支持回放
            return R.error("直连摄像机不支持回放");
        }
    }

    private static R handleRecords(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        JSONObject cs = JSONObject.parseObject(camera.getRemark());
        if (cs.containsKey("type") && JinzhiConst.TYPE_GB.equalsIgnoreCase(cs.getString("type"))) {
            //国标，参数是日期 2024-08-13

            String api = "http://" + netDevice.getIp() + ":8080/agapi/device/recordfind?deviceID=" + camera.getCode() + "&findTimeDay=" + controlVo.getValue();
            //获取任务列表
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + getToken(netDevice));
            JinzhiRecord result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), JinzhiRecord.class).getBody();
            if (!result.success() || result.getList() == null || result.getList().length == 0) {
                return R.error("未查询到录像");
            }
            for (VideoRecord videoRecord : result.getList()) {
                recordName(videoRecord);
            }
            return R.success(result.getList());
        } else if (cs.containsKey("type") && JinzhiConst.TYPE_DHSDKS.equalsIgnoreCase(cs.getString("type"))) {
            //大华SDK，参数是日期 2024-08-13
            String api = "http://" + netDevice.getIp() + ":8080/agapi/device/recordfind?deviceID=" + camera.getCode() + "&findTimeDay=" + controlVo.getValue();
            //获取任务列表
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + getToken(netDevice));
            JinzhiRecord result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), JinzhiRecord.class).getBody();
            if (!result.success() || result.getList() == null || result.getList().length == 0) {
                return R.error("未查询到录像");
            }
            for (VideoRecord videoRecord : result.getList()) {
                recordName(videoRecord);
            }
            log.info("records for {} @{}: {}", camera.getCode(), controlVo.getValue(), JSONObject.toJSONString(result));
            return R.success(result.getList());
        } else {
            //直连不支持回放
            return R.error("直连摄像机不支持回放");
        }
    }

    private static R handleSnap(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        byte[] result = snap(netDevice, camera);
        if (result == null) {
            return R.error("截图失败");
        }
        TempFileStore.SavedFile saved = SpringUtils.getBean(TempFileStore.class)
                .save(result, "snap", "jpg");
        return R.success(saved.getHttpUrl());
    }

    private static R handlePtz(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        //云台控制
        Session session = JinZhiHandler.sessionMap.get(netDevice.getCode());
        if (session == null) {
            return R.error("网关不线");
        }
        JSONObject msg = new JSONObject();
        msg.put("msgID", "01");
        msg.put("action", "StartMoveReq");

        JSONObject param = new JSONObject();
        param.put("deviceID", camera.getCode());
        param.put("x", 0);
        param.put("y", 0);
        param.put("z", 0);
        msg.put("param", param);
        String[] vs = controlVo.getValue().split(":");
        int speed = 1;
        if (vs.length > 1 && vs[1].equalsIgnoreCase("null")) {
            speed = Integer.parseInt(vs[1]);
        }
        switch (vs[0].toLowerCase()) {
            case "left":
                param.put("x", speed);
                break;
            case "left_up":
                param.put("x", speed);
                param.put("y", speed);
                break;
            case "left_down":
                param.put("x", speed);
                param.put("y", -1 * speed);
                break;
            case "right":
                param.put("x", -1 * speed);
                break;
            case "right_up":
                param.put("x", -1 * speed);
                param.put("x", speed);
                break;
            case "right_down":
                param.put("x", -1 * speed);
                param.put("y", -1 * speed);
                break;
            case "up":
                param.put("y", speed);
                break;
            case "down":
                param.put("y", -1 * speed);
                break;
            case "in":
                param.put("z", speed);
                break;
            case "out":
                param.put("z", -1 * speed);
                break;
            case "stop":
                msg.put("action", "StopMoveReq");
                msg.remove("param");
                break;
            default:
                break;
        }

        JinzhiWebSocket.sendMsg(session, msg.toJSONString());
        //设置延迟停止
        String ptzDelay = platform.getConfigStr("ptzDelay", "3");
        executor.schedule(() -> {
            try {
                msg.put("action", "StopMoveReq");
                JinzhiWebSocket.sendMsg(session, msg.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Integer.parseInt(ptzDelay), TimeUnit.SECONDS);
        return R.success();
    }

    private static R handleDhStart(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        //开始大华对讲
        Session session = JinZhiHandler.sessionMap.get(netDevice.getCode());
        if (session == null) {
            return R.error("网关不线");
        }
        JSONObject msg = new JSONObject();
        msg.put("msgID", camera.getCode());
        msg.put("action", "DHIPCTalkStartReq");

        JSONObject param = new JSONObject();
        param.put("deviceID", camera.getCode());
        param.put("wsurl", controlVo.getValue());
        msg.put("param", param);

        JinzhiWebSocket.sendMsg(session, msg.toJSONString());
        return R.success();
    }

    private static R handleDhStop(ZaSysDevice netDevice, ZaSysDevice camera, ControlVo controlVo) {
        //停止大华对讲
        Session session = JinZhiHandler.sessionMap.get(netDevice.getCode());
        if (session == null) {
            return R.error("网关不线");
        }
        JSONObject msg = new JSONObject();
        msg.put("msgID", camera.getCode());
        msg.put("action", "DHIPCTalkStopReq");

        JSONObject param = new JSONObject();
        param.put("deviceID", camera.getCode());
        msg.put("param", param);

        JinzhiWebSocket.sendMsg(session, msg.toJSONString());
        return R.success();
    }


    /**
     * 流地址前缀
     *
     * @param useSsl
     * @param netDevice
     * @param protocol
     * @return
     */
    private static String playUrlPrefix(Boolean useSsl, ZaSysDevice netDevice, String protocol) {
        if (StrUtil.isEmpty(protocol)) {
            protocol = "ws";
        }
        if (useSsl) {
            protocol += "s";
        }
        return protocol + "://" + netDevice.getIp() + ":" + (useSsl ? 8183 : 8083) + "/rtp/";
    }

    /**
     * 登录网关，获取令牌
     *
     * @param netDevice device
     * @return token
     */
    private static String getToken(ZaSysDevice netDevice) {
        String cacheKey = "jinzhi:token:" + netDevice.getId();
        String token = cache.getCacheObject(cacheKey);
        if (token != null) {
            log.info("{} 的令牌 {} 可用", netDevice.getId(), token);
            return token;
        }
        String url = "http://" + netDevice.getIp() + ":8080/agapi/login";
        Map<String, Object> map = new HashMap<>();
        map.put("username", "admin");
        map.put("password", "");
        String retStr = HttpUtils.post(url, JSONObject.toJSONString(map));
        JSONObject ret = JSONObject.parseObject(retStr);
        if (!ret.containsKey("param")) {
            return null;
        }
        String random = ret.getJSONObject("param").getString("random");
        String password = Md5Utils.hash("zhianadmin:" + random);
        map.put("password", password);
        retStr = HttpUtils.post(url, JSONObject.toJSONString(map));
        ret = JSONObject.parseObject(retStr);
        if (!ret.getString("result").equalsIgnoreCase("success")) {
            return null;
        }
        token = ret.getString("LX_TOKEN");
        if (!StrUtil.isEmpty(token)) {
            cache.setCacheObject(cacheKey, token, 25, TimeUnit.MINUTES);
            log.info("{} 的获取新令牌 {}", netDevice.getId(), token);
        }
        return token;
    }

    /**
     * 生成文件名
     *
     * @param videoRecord record
     */
    private static void recordName(VideoRecord videoRecord) {
        String name = "";
        Date dt = DateUtils.addHours(videoRecord.getStartTime(), -8);
        name = DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, videoRecord.getStartTime());
        //videoRecord.setStartTime(dt);
        dt = DateUtils.addHours(videoRecord.getEndTime(), -8);
        name += "_" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, videoRecord.getEndTime());
        //videoRecord.setEndTime(dt);
        videoRecord.setName(name);
    }

    /**
     * 摄像机截图
     *
     * @param gateway
     * @param camera
     */
    private static byte[] snap(ZaSysDevice gateway, ZaSysDevice camera) {
        String api = "http://" + gateway.getIp() + ":8080/agapi/snap?deviceID=" + camera.getCode() + "&sec=0&time=0";

        try {
            URL url = new URL(api);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置自定义请求头
            connection.setRequestProperty("Authorization", "Bearer " + getToken(gateway));

            // 设置超时时间
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            InputStream inputStream = connection.getInputStream();
            // 使用ImageIO读取流
            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new IOException("Failed to decode image from stream");
            }

            // 保存图片为jpg
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            connection.disconnect();
            return baos.toByteArray();

            //byte[] result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), byte[].class).getBody();
            //return result;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("jinzhi snap {} failed: {}", api, e.getLocalizedMessage());
        }
        return null;
    }
}
