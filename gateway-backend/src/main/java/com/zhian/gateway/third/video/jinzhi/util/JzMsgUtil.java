package com.zhian.gateway.third.video.jinzhi.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.consts.MessageConstants;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.video.jinzhi.JinZhiHandler;
import com.zhian.gateway.third.video.jinzhi.JinzhiConst;
import com.zhian.gateway.third.video.jinzhi.JinzhiWebSocket;
import com.zhian.gateway.third.video.jinzhi.bo.JzMessage;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiKey;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiWsReq;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiWsResp;
import com.zhian.gateway.third.video.vo.JinZhiGbDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.websocket.Session;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 金智视频网关设备相关工具类
 *
 * @author tongwenjin
 * @since 2026/8/21
 */
@Slf4j
public class JzMsgUtil {

    private static final Pattern STREAM_IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

    private static IZaSysDeviceService deviceService;

    private static ZaSysPlatform platform;

    public static void init(ZaSysPlatform platform) {
        JzMsgUtil.platform = platform;

        deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
    }

    public static void processMessage(JzMessage msg) {
        Session session = msg.getSession();
        String message = msg.getMessage();

        JinzhiWsReq req = JSON.parseObject(message, JinzhiWsReq.class);

        MsgProcessContext.start(message, platform);

        switch (req.getAction()) {
            case "GetDynKeyReq":
                JzMsgUtil.handleDynKeyReq(session, req);
                break;
            case "AuthenticationReq":
                JzMsgUtil.handleAuthReq(session, req);
                break;
            case "HeartBeatReq":
                JzMsgUtil.handleHeartReq(session, req);
                break;
            case "DeviceListGetResp":
                break;
            case "SendDeviceStatusReq":
                JzMsgUtil.handleDevStatusReq(session, req);
                break;
            case "DeviceListReq":
                JzMsgUtil.handleDevListReq(session, req);
                break;
            case "DeviceAddReq":
                JzMsgUtil.handleDevAddReq(session, req);
                break;
            case "DeviceDelReq":
                JzMsgUtil.handleDevDelReq(session, req);
                break;
            case "DeviceDelAllReq":
                JzMsgUtil.handleDelAllReq(session, req);
                break;
            case "AIEventNotify":
                JzMsgUtil.handleAiEvent(session, req);
                break;
            default:
                log.error("no thing to do： {}", req.getAction());
        }
    }

    public static void handleDynKeyReq(Session session, JinzhiWsReq req) {
        //DynKey获取
        JSONObject reqParam = req.getObject();
        String mac = reqParam.getString("MAC");
        if (StrUtil.isEmpty(mac)) {
            sendError(session, "GetDynKeyResp", null);
        } else {
            JinzhiKey k = JinzhiWebSocket.KEY_MAP.get(session.getId());
            k.setMac(mac);
            k.setTime(DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD_HH_MM_SS));
            k.setKey(IdUtil.fastSimpleUUID());
            k.setModel(reqParam.getString("brandDeviceMode"));
            JinzhiWebSocket.KEY_MAP.put(session.getId(), k);
            JSONObject param = new JSONObject();
            param.put("dynKey", k.getKey());
            param.put("result", JinzhiWsResp.RESULT_SUCCESS);
            param.put("time", k.getTime());
            sendSuccess(session, "GetDynKeyResp", param);
        }
    }

    public static void handleAuthReq(Session session, JinzhiWsReq req) {
        JinzhiKey key = JinzhiWebSocket.KEY_MAP.get(session.getId());
        //授权码校验
        JSONObject reqParam = req.getObject();
        String authorization = reqParam.getString("authorization");
        if (StrUtil.isEmpty(authorization)) {
            sendError(session, "AuthenticationResp", null);
            return;
        } else {
            JSONObject param = new JSONObject();
            param.put("result", JinzhiWsResp.RESULT_SUCCESS);
            sendSuccess(session, "AuthenticationResp", param);
        }
        JinzhiWebSocket.GATE_MAP.put(session.getId(), syncGateway(key, session));
        //连接成功后，下发指令，要求上报设备信息
        JinzhiWebSocket.sendMsg(session, JinzhiWebSocket.DEVICE_LIST_GET_REQ);
    }

    public static void handleHeartReq(Session session, JinzhiWsReq req) {
        //心跳
        JSONObject param = new JSONObject();
        param.put("result", JinzhiWsResp.RESULT_SUCCESS);
        sendSuccess(session, "HeartBeatResp", param);
        JinzhiKey key = JinzhiWebSocket.KEY_MAP.get(session.getId());
        syncGateway(key, session);
    }

    public static void handleDevListReq(Session session, JinzhiWsReq req) {
        //任务的设备列表,先保存到缓存中，通过SendDeviceStatusReq确定已经添加的任务，自动同步
        if (StrUtil.isEmpty(req.getParam())) {
            return;
        }
        ZaSysDevice net = JinzhiWebSocket.GATE_MAP.get(session.getId());
        ZaSysDevice odc = new ZaSysDevice();
        odc.setNet(net.getCode());
        Map<String, ZaSysDevice> removeCameras = deviceService.selectZaSysDeviceList(odc).stream()
                .collect(Collectors.toMap(ZaSysDevice::getCode, ZaSysDevice -> ZaSysDevice));
        JSONArray list = JSONArray.parseArray(req.getParam());
        for (int i = 0; i < list.size(); i++) {
            JSONObject camera = list.getJSONObject(i);
            String code = camera.getString(JinzhiConst.FIELD_DEVICE_ID);
            ZaSysDevice dc = removeCameras.get(code);
            if (dc != null) {
                removeCameras.remove(code);
            }
            addCamera(net, camera);
        }
        // 不存在的摄像机 - 直接删除
        for (ZaSysDevice dc : removeCameras.values()) {
            deviceService.deleteZaSysDeviceById(dc.getId());
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDevice(
                            dc, MessageConstants.MSG_TYPE_DEVICE_DEL, "视频网关同步发现设备已删除"
                    )
            );
        }
    }

    public static void handleDevStatusReq(Session session, JinzhiWsReq req) {
        //设备状态上传请求,任务摄像机的状态
        if (StrUtil.isEmpty(req.getParam())) {
            return;
        }
        JSONArray status = JSONArray.parseArray(req.getParam());
        ZaSysDevice net = JinzhiWebSocket.GATE_MAP.get(session.getId());
        for (int i = 0; i < status.size(); i++) {
            JSONObject camera = status.getJSONObject(i);
            // 此处新注册的设备可能存在并发问题 ,导致无法查询到设备数据(故尝试休眠2s后再尝试查询设备信息一次)
            ZaSysDevice dc = deviceService.selectZaSysDeviceByCode(
                    camera.getString(JinzhiConst.FIELD_DEVICE_ID), net.getCode()
            );
            if (dc == null) {
                // 休眠2s后再查询设备是否存在
                ThreadUtil.sleep(2000L);
                dc = deviceService.selectZaSysDeviceByCode(
                        camera.getString(JinzhiConst.FIELD_DEVICE_ID), net.getCode()
                );
                if (dc == null) {
                    continue;
                }
            }
            //同步状态
            checkCameraStatus(dc, camera.getString(JinzhiConst.FIELD_STATUS));
        }
    }

    public static void handleDevAddReq(Session session, JinzhiWsReq req) {
        //添加新的任务设备信息，实时同步
        ZaSysDevice net = JinzhiWebSocket.GATE_MAP.get(session.getId());

        // 判断网关是否存在，不存在则同步新增一条
        long netCount = deviceService.count(
                Wrappers.lambdaQuery(ZaSysDevice.class).eq(ZaSysDevice::getCode, net.getCode())
        );
        if (netCount < 1) {
            deviceService.save(net);
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDevice(net, MessageConstants.MSG_TYPE_DEVICE_ADD, "视频网关同步")
            );
        }

        ZaSysDevice camera = addCamera(net, req.getObject());
        checkCameraStatus(camera, req.getObject().getString(JinzhiConst.FIELD_STATUS));
    }

    public static void handleDevDelReq(Session session, JinzhiWsReq req) {
        //删除设备，实时同步
        ZaSysDevice net = JinzhiWebSocket.GATE_MAP.get(session.getId());
        JSONObject ps = JSONObject.parseObject(req.getParam());
        deleteCamera(net.getCode(), ps.getString(JinzhiConst.FIELD_DEVICE_ID));
        log.info("删除摄像机 {} - {}", net.getCode(), ps.getString(JinzhiConst.FIELD_DEVICE_ID));
    }

    public static void handleDelAllReq(Session session, JinzhiWsReq req) {
        //删除全部设备，实时同步
        ZaSysDevice net = JinzhiWebSocket.GATE_MAP.get(session.getId());
        MsgProcessContext.getProcessInfo().setDevice(net);
        deleteCamera(net.getCode(), null);
        log.info("删除网关全部摄像机 {}", net.getCode());
    }

    public static void handleAiEvent(Session session, JinzhiWsReq req) {
        // 解析json数据
        JSONObject aiEvent = JSON.parseObject(JSON.toJSONString(req));
        JSONObject param = aiEvent.getJSONObject("param");
        JSONObject event = param.getJSONObject("event");

        if (event.getJSONArray("targets").isEmpty()) {
            handleNoTargetEvent(session, param);
            return;
        } else {
            String deviceId = param.getString("deviceID");
            // 事件类型
            String eventType = event.getString("eventID");

            // 只处理第一个target元素
            JSONObject target = event.getJSONArray("targets").getJSONObject(0);

            String score = target.getString("score");

            if (Integer.parseInt(score) < JinzhiWebSocket.AI_SCORE) {
                log.warn("当前AI事件置信度:{} , 小于告警阈值{}, 忽略该事件!", score, JinzhiWebSocket.AI_SCORE);
                return;
            } else {
                log.info("执行AI事件 , 置信度:{} !", score);
            }
            if (target.containsKey("eventID")) {
                eventType = target.getString("eventID");
            }
            if (target.containsKey("deviceID")) {
                deviceId = target.getString("deviceID");
            }
            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(deviceId, JinzhiWebSocket.GATE_MAP.get(session.getId()).getCode());
            if (zaSysDevice == null) {
                log.warn("未注册的摄像机 {} ", deviceId);
                return;
            }
            // 保存图片
            String alarmImg = null;
            String picID = target.getString("picID");
            JSONArray pics = event.getJSONArray("pics");
            for (int j = 0; j < pics.size(); j++) {
                JSONObject pic = pics.getJSONObject(j);
                String picBase64 = pic.getString("image");
                if (pic.getString("picID").equals(picID)) {
                    alarmImg = drawRedRect(deviceId, eventType, picBase64,
                            0,
                            0,
                            target.getInteger("w"),
                            target.getInteger("h")
                    );
                }
            }

            event.remove("pics");

            //  告警类型
            TypeMappingService mappingService = SpringUtils.getBean(TypeMappingService.class);
            Optional<ZaAlarmType> type = mappingService.resolveAlarmType("jinzhi", eventType);
            if (!type.isPresent()) {
                log.error("处理金智AI 事件失败:{}未注册!", eventType);
                return;
            }

            MsgProcessContext.addMsg(
                    MessageBuilder.buildAlarm(zaSysDevice, type.get(), "", alarmImg)
            );
        }
    }

    /**
     * {"action":"AIEventNotify",
     * "param":{"taskID":"af464945247044f7be9173fe76e90484","deviceID":"87446fe20795be4010645ecba413c2c1","deviceName":"胜东花园一期监控室",
     * "event":{"uuid":"5df073c22501455582f87495dacda2be","taskID":"af464945247044f7be9173fe76e90484","eventID":"JZLX-PersonOut","eventName":"人员离岗","time":"2026/03/12 10:26:19","customID":"","attrs":{},"targets":[],
     * "pics":[{"picID":"3e4dd4fc18894abc8e766cee50504877","w":1920,"h":1080,"deviceID":"87446fe20795be4010645ecba413c2c1","image":"
     *
     * @param session the session
     * @param param   the param
     */
    public static void handleNoTargetEvent(Session session, JSONObject param) {
        String deviceId = param.getString("deviceID");
        ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(deviceId, JinzhiWebSocket.GATE_MAP.get(session.getId()).getCode());
        if (zaSysDevice == null) {
            log.warn("未注册的摄像机 {} ", deviceId);
            return;
        }
        JSONObject event = param.getJSONObject("event");
        // 事件类型
        String eventType = event.getString("eventID");
        // 保存图片
        String alarmImg = null;
        JSONArray pics = event.getJSONArray("pics");
        for (int j = 0; j < pics.size(); j++) {
            JSONObject pic = pics.getJSONObject(j);
            String picBase64 = pic.getString("image");
            if (pic.getString("deviceID").equals(deviceId)) {
                alarmImg = drawRedRect(deviceId, eventType, picBase64, 0, 0,
                        pic.getInteger("w"),
                        pic.getInteger("h")
                );
            }
        }

        //  告警类型
        TypeMappingService mappingService = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaAlarmType> type = mappingService.resolveAlarmType("jinzhi", eventType);
        if (!type.isPresent()) {
            log.error("处理金智AI 事件失败:{}未注册!", eventType);
            return;
        }

        MsgProcessContext.addMsg(
                MessageBuilder.buildAlarm(zaSysDevice, type.get(), "", alarmImg)
        );
    }

    /**
     * 告警图片
     *
     * @param deviceId  设备
     * @param eventType 事件类型
     * @param base64    图片BASE64
     * @param x         X坐标
     * @param y         Y坐标
     * @param width     宽度
     * @param height    高度
     * @return String
     */
    public static String drawRedRect(String deviceId, String eventType, String base64, int x, int y, int width, int height) {
        if (StrUtil.isBlank(base64)) {
            return base64;
        }
        // 输出图路径
        String fileName = "/jinzhi/" + deviceId + "_" + eventType + "_" + System.currentTimeMillis() + ".jpg";
        String outputPath = ZhianConfig.getProfile() + fileName;

        FileUtil.mkParentDirs(outputPath);

        // 读取图片
        BufferedImage image = null;

        try {
            image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (x != 0 || y != 0) {
            // 获取图像绘图对象
            Graphics2D g2d = image.createGraphics();

            // 设置红色和线宽
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(5));

            // 画矩形
            g2d.drawRect(x, y, width, height);

            // 清理资源
            g2d.dispose();
        }


        // 保存修改后的图像
        try {
            ImageIO.write(image, "jpeg", new File(outputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("绘制完成，保存为：{}", outputPath);

        return Constants.RESOURCE_PREFIX + fileName;
    }

    /**
     * 删除摄像机
     *
     * @param net      the net
     * @param cameraId the camera id
     */
    public static void deleteCamera(String net, String cameraId) {
        ZaSysDevice dc = new ZaSysDevice();
        dc.setType(DeviceType.CAMERA);
        dc.setNet(net);
        if (StrUtil.isNotEmpty(cameraId)) {
            dc.setCode(cameraId);
        }
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        for (ZaSysDevice camera : list) {
            if (MsgProcessContext.getProcessInfo().getDevice() == null) {
                MsgProcessContext.getProcessInfo().setDevice(camera);
            }
            deviceService.deleteZaSysDeviceById(camera.getId());
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDevice(camera, MessageConstants.MSG_TYPE_DEVICE_DEL, "视频网关同步删除设备")
            );
        }
    }

    /**
     * 同步设备状态。
     * 上报 online 时始终取消待推送的离线告警，避免短时闪断后仍误推离线。
     *
     * @param camera the camera
     * @param status the status
     */
    public static void checkCameraStatus(ZaSysDevice camera, String status) {
        boolean reportOnline = "online".equalsIgnoreCase(status);
        // 默认状态为在线状态
        if (StrUtil.isEmpty(camera.getOnline())) {
            camera.setOnline("1");
        }
        boolean currentOnline = "1".equalsIgnoreCase(camera.getOnline());

        if (reportOnline) {
            if (!currentOnline) {
                camera.setOnline("1");
                deviceService.updateZaSysDevice(camera);
                MsgProcessContext.addMsg(
                        MessageBuilder.buildDeviceState(camera, "1", "设备状态消息发现设备在线")
                );
            } else if (new Random().nextInt(5) < 2) {
                MsgProcessContext.addMsg(
                        MessageBuilder.buildDeviceState(camera, "1", "设备状态消息发现设备在线")
                );
            }
            return;
        }

        // 上报离线
        if (currentOnline) {
            camera.setOnline("0");
            deviceService.updateZaSysDevice(camera);
            log.info("十分钟之后执行设备({})离线告警推送!", camera.getCode());
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDeviceState(camera, "0", "设备状态消息发现设备离线")
            );
//            DelayTaskUtil.submit(
//                    camera.getCode(),
//                    () -> {
//                        videoHelper.pushState(camera, AlarmType.OFFLINE);
//                        log.info("十分钟之前发现设备({})离线告警,执行推送!", camera.getCode());
//                    }
//            );
        } else if (new Random().nextInt(5) < 2) {
//            videoHelper.pushState(camera, AlarmType.OFFLINE);
        }
    }

    /**
     * 自动注册网关信息
     *
     * @param key     the key
     * @param session the session
     * @return za sys device
     */
    public static ZaSysDevice syncGateway(JinzhiKey key, Session session) {
        String ip = key.getIp();
        String mac = key.getMac();
        String model = key.getModel();

        HashMap<Object, Object> remark = new HashMap<>();
        remark.put("ip", ip);
        remark.put("mac", mac);
        remark.put("model", model);

        // 同步网关设备信息
        SyncDevice syncDevice = SyncDevice.builder()
                .code(mac)
                .name("视频网关 " + model + " " + ip)
                .model(model)
                .typeCode(DeviceType.VAG)
                .pfCode("jinzhi")
                .wireless("1")
                .ip(ip)
                .remark(JSONObject.toJSONString(remark)).build();
        ZaSysDevice gateway = DeviceUtil.syncDevice(syncDevice);

        JinZhiHandler.sessionMap.put(mac, session);

        log.info("视频网关 {}-{}-{} 已同步", model, ip, mac);
        return gateway;
    }

    /**
     * 同步摄像机
     *
     * @param net    the net
     * @param camera {"deviceID":"2CB165135C7F517647732B0430B869CF","name":"65","type":"ONVIF","ip":"192.168.1.65","username":"admin","password":"zhian12345","streamUrl":"rtsp://admin:zhian12345@192.168.1.65:554/Streaming/Channels/101?transportmode=unicast&profile=Profile_1","streamSubUrl":"rtsp://admin:zhian12345@192.168.1.65:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_2","longitude":"","latitude":"","address":"","modifyTime":"2024/07/01 13:52:54","exid1400":"","exidgb":"32412314324","scenes":"","status":"online","resolution":"","fps":-1,"videoCode":"","audioCode":""
     * @return the za sys device
     */
    public static synchronized ZaSysDevice addCamera(ZaSysDevice net, JSONObject camera) {
        //扩展信息
        JinZhiGbDevice device = new JinZhiGbDevice();
        device.setType(camera.getString(JinzhiConst.FIELD_TYPE));
        if (device.getType().equalsIgnoreCase(JinzhiConst.TYPE_GB)) {
            device.setDeviceID(camera.getString("subGBDevID"));
            device.setChannelID(camera.getString("subGBChannelID"));
            device.setStreamUrl("rtsp://" + net.getIp() + ":554/rtp/" + device.getDeviceID() + "_" + device.getChannelID());
        } else {

            String ip = camera.getString(JinzhiConst.FIELD_IP);
            String streamUrl = camera.getString("streamUrl");
            // 非ONVIF 尝试从流地址取IP
            if (!device.getType().equalsIgnoreCase(JinzhiConst.TYPE_ONVIF)) {
                Matcher matcher = STREAM_IP_PATTERN.matcher(streamUrl);
                ip = matcher.find() ? matcher.group() : ip;
            }

            device.setIp(ip);
            device.setStreamUrl(streamUrl);
            device.setDeviceID(camera.getString(JinzhiConst.FIELD_DEVICE_ID));
            device.setUsername(camera.getString("username"));
            device.setPassword(camera.getString("password"));
            device.setChannelCount(camera.getInteger("channelCount"));
            device.setChannelID(camera.getString("channelNo"));
        }

        String deviceId = camera.getString(JinzhiConst.FIELD_DEVICE_ID);
        String online = camera.getString(JinzhiConst.FIELD_STATUS).equalsIgnoreCase(JinzhiConst.ONLINE) ? "1" : "0";


        // 同步云盒设备信息
        SyncDevice syncDevice = SyncDevice.builder()
                .code(deviceId)
                .net(net.getCode())
                .name(camera.getString(JinzhiConst.FIELD_NAME))
                .typeCode(DeviceTypeEnum.CAMERA.getCode())
                .pfCode("jinzhi")
                .wireless("1")
                .ip(device.getIp())
                .online(online)
                .remark(JSONObject.toJSONString(device)).build();
        return DeviceUtil.syncDevice(syncDevice);
    }

    /**
     * 响应错误信息
     *
     * @param session the session
     * @param action  the action
     * @param param   the param
     */
    public static void sendError(Session session, String action, JSONObject param) {
        JinzhiWsResp res = JinzhiWsResp.fail(action);
        res.setParam(param);
        try {
            String content = JSONObject.toJSONString(res);
            log.debug("response error to jinzhi ws: {}", content);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(content.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 响应正确结果
     *
     * @param session the session
     * @param action  the action
     * @param param   the param
     */
    public static void sendSuccess(Session session, String action, JSONObject param) {
        JinzhiWsResp res = JinzhiWsResp.success(action);
        res.setParam(param);
        try {
            String content = JSONObject.toJSONString(res);
            log.debug("response success to jinzhi ws: {}", content);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(content.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
