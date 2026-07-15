package com.zhian.gateway.third.video.jinzhi;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.ExceptionUtil;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.sign.Base64;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.UUID;
import com.zhian.gateway.framework.web.websocket.AbstractWebSocket;
import com.zhian.gateway.framework.web.websocket.config.WebSocketSessionConfigurator;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.system.service.ISysConfigService;
import com.zhian.gateway.third.common.constants.AlarmType;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.video.VideoHelper;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiKey;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiWsReq;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiWsResp;
import com.zhian.gateway.third.video.vo.JinZhiGbDevice;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 对接金智网关的websocket连接，接收上报的设备信息
 * 客户端连接 ws://192.168.1.110:9999/ws/420200
 * *
 *
 * @author zhian
 */
@ServerEndpoint(value = "/jz/ws", configurator = WebSocketSessionConfigurator.class)
@Component
@Slf4j
@SuppressWarnings("CallToPrintStackTrace")
public class JinzhiWebSocket extends AbstractWebSocket {

    private static Integer AI_SCORE = 80;

    /**
     * The constant keyMap.
     */
    private static final Map<String, JinzhiKey> keyMap = new ConcurrentHashMap<>();
    /**
     * The constant gateMap.
     */
    private static final Map<String, ZaSysDevice> gateMap = new ConcurrentHashMap<>();
    /**
     * The Video helper.
     */
    private VideoHelper videoHelper;
    /**
     * The Device service.
     */
    private IZaSysDeviceService deviceService;
    /**
     * The Jin zhi handler.
     */
    private JinZhiHandler jinZhiHandler;
    /**
     * The constant DEVICE_LIST_GET_REQ.
     */
    public static final String DEVICE_LIST_GET_REQ = "{\"version\":\"1.0.0\",\"action\":\"DeviceListGetReq\",\"msgID\":\"00\",\"param\":[]}";

    private static final  Pattern STREAM_IP_PATTERN = Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

    /**
     * 手工注入service
     */
    private synchronized void initService() {
        if (videoHelper == null) {
            videoHelper = SpringUtils.getBean(VideoHelper.class);
        }
        if (deviceService == null) {
            deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
        }
        if (jinZhiHandler == null) {
            jinZhiHandler = SpringUtils.getBean(JinZhiHandler.class);
        }
        String score = SpringUtils.getBean(ISysConfigService.class).selectConfigByKey("jz.ai.score");
        if(StrUtil.isNotBlank(score) && StrUtil.isNumeric(score)) {
            AI_SCORE = Integer.parseInt(score);
        }
    }

    /**
     * On open.
     *
     * @param session the session
     * @param config  the config
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 在线数加1
        calcOnlineCount(1);
        JinzhiKey key = new JinzhiKey();
        key.setIp(getClientIpAddress(config));
        keyMap.put(session.getId(), key);
        log.info("有新连接加入{}！当前在线网关数为{}", key.getIp(), getOnlineCount());
    }

    /**
     * On close.
     *
     * @param session     the session
     * @param closeReason the close reason
     * @throws IOException the io exception
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws IOException {
        //在线数减1
        calcOnlineCount(-1);
        log.info("有一连接关闭！当前在线网关数为{}: {}", getOnlineCount(), closeReason);
        ZaSysDevice gateway = gateMap.get(session.getId());
        if (gateway != null) {
            jinZhiHandler.removeGateway(gateway);
        }
        keyMap.remove(session.getId());
        gateMap.remove(session.getId());
    }

    /**
     * On error.
     *
     * @param session the session
     * @param t       the t
     */
    @OnError
    public void onError(Session session, Throwable t) {
        try {
            if (!session.isOpen()) {
                log.error("session 连接发生异常,主动关闭连接!,异常信息:{}", ExceptionUtil.getExceptionMessage(t));
                this.onClose(session, null);
            } else {
                log.error("Ws error occurred , msg : {}", ExceptionUtil.getExceptionMessage(t));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to close ws connection , msg :{}", ExceptionUtil.getExceptionMessage(e));
        }
    }

    /**
     * On message.
     *
     * @param session the session
     * @param bytes   the binary message
     */
    @OnMessage(maxMessageSize = 5242880)
    public void onMessage(Session session, ByteBuffer bytes) {
        //初始化
        initService();

        String message = new String(bytes.array());
        log.info("====receive: " + message);
        try {
            // 异步处理
            CompletableFuture
                    .runAsync(() -> processMessage(session, message))
                    .exceptionally(e -> {
                        log.error("处理金智消息失败:{}", e.getMessage());
                        e.printStackTrace();
                        return null;
                    });
        } catch (Exception e) {
            log.error("处理视频网关消息时异常： {}", e.getMessage());
        }
    }

    /**
     * 消息单独处理，在业务层采用异常处理，解决报错后，ws无限重连的问题
     *
     * @param session the session
     * @param message the message
     */
    private void processMessage(Session session, String message) {
        JinzhiWsReq req = JSON.parseObject(message, JinzhiWsReq.class);
        if (req.getAction().equalsIgnoreCase("GetDynKeyReq")) {
          /*
            if(!req.getVersion().equalsIgnoreCase(JinzhiWsResp.VERSION)){
                JSONObject param = new JSONObject();
                param.put("result", JinzhiWsResp.RESULT_FAIL);
                param.put("msg", "不支持的版本号");
                sendError(session, "GetDynKeyResp", param);
            }
           */

            //DynKey获取
            JSONObject reqParam = req.getObject();
            String mac = reqParam.getString("MAC");
            if (StringUtils.isEmpty(mac)) {
                sendError(session, "GetDynKeyResp", null);
            } else {
                JinzhiKey k = keyMap.get(session.getId());
                k.setMac(mac);
                k.setTime(DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD_HH_MM_SS));
                k.setKey(UUID.fastUUID().toString(true));
                k.setModel(reqParam.getString("brandDeviceMode"));
                keyMap.put(session.getId(), k);
                JSONObject param = new JSONObject();
                param.put("dynKey", k.getKey());
                param.put("result", JinzhiWsResp.RESULT_SUCCESS);
                param.put("time", k.getTime());
                sendSuccess(session, "GetDynKeyResp", param);
            }
        }
        else if (req.getAction().equalsIgnoreCase("AuthenticationReq")) {
            JinzhiKey key = keyMap.get(session.getId());
            //授权码校验
            JSONObject reqParam = req.getObject();
            String authorization = reqParam.getString("authorization");
            if (StringUtils.isEmpty(authorization)) {
                sendError(session, "AuthenticationResp", null);
                return;
            } else {
                JSONObject param = new JSONObject();
                param.put("result", JinzhiWsResp.RESULT_SUCCESS);
                sendSuccess(session, "AuthenticationResp", param);
            }
            ZaSysDevice gateway = addGateway(key.getMac(), key.getModel(), key.getIp());
            gateMap.put(session.getId(), gateway);
            jinZhiHandler.addGateway(gateway, session);
            //连接成功后，下发指令，要求上报设备信息
            sendMsg(session, DEVICE_LIST_GET_REQ);
        }
        else if (req.getAction().equalsIgnoreCase("HeartBeatReq")) {
            //心跳
            JSONObject param = new JSONObject();
            param.put("result", JinzhiWsResp.RESULT_SUCCESS);
            sendSuccess(session, "HeartBeatResp", param);
        }
        else if (req.getAction().equalsIgnoreCase("DeviceListGetResp")) {
            //响应握手成功之后，下发的DeviceListGetReq指令，没有上报实质数据
            log.debug("等待上报数据");
        }
        else if (req.getAction().equalsIgnoreCase("DeviceListReq")) {
            //任务的设备列表,先保存到缓存中，通过SendDeviceStatusReq确定已经添加的任务，自动同步
            if (StringUtils.isEmpty(req.getParam())) {
                return;
            }
            ZaSysDevice net = gateMap.get(session.getId());
            ZaSysDevice odc = new ZaSysDevice();
            odc.setNet(net.getCode());
            Map<String, ZaSysDevice> cameraMap = deviceService.selectZaSysDeviceList(odc).stream().collect(Collectors.toMap(ZaSysDevice::getCode, ZaSysDevice -> ZaSysDevice));
            JSONArray list = JSONArray.parseArray(req.getParam());
            for (int i = 0; i < list.size(); i++) {
                JSONObject camera = list.getJSONObject(i);
                String code = camera.getString(JinzhiConst.FIELD_DEVICE_ID);
                ZaSysDevice dc = cameraMap.get(code);
                if (dc != null) {
                    cameraMap.remove(code);
                }
                addCamera(net, camera);
            }
            //同步删除摄像机
            for (ZaSysDevice dc : cameraMap.values()) {
                deviceService.deleteZaSysDeviceById(dc.getId());
                // TODO
                // jinZhiHandler.pushDevice(dc, MqMessage.DEVICE_REMOVE, null);
            }
        }
        else if (req.getAction().equalsIgnoreCase("SendDeviceStatusReq")) {
            //设备状态上传请求,任务摄像机的状态
            if (StringUtils.isEmpty(req.getParam())) {
                return;
            }
            JSONArray status = JSONArray.parseArray(req.getParam());
            ZaSysDevice net = gateMap.get(session.getId());
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
        else if (req.getAction().equalsIgnoreCase("DeviceAddReq")) {
            //添加新的任务设备信息，实时同步
            ZaSysDevice net = gateMap.get(session.getId());

            // 判断网关是否存在，不存在则同步新增一条
            long netCount = deviceService.count(
                    Wrappers.lambdaQuery(ZaSysDevice.class).eq(ZaSysDevice::getCode, net.getCode())
            );
            if (netCount < 1) {
                deviceService.save(net);
                // TODO
                // jinZhiHandler.pushDevice(net, MqMessage.DEVICE_ADD, net.getRemark());
            }

            ZaSysDevice camera = addCamera(net, req.getObject());
            checkCameraStatus(camera, req.getObject().getString(JinzhiConst.FIELD_STATUS));
        }
        else if (req.getAction().equalsIgnoreCase("DeviceDelReq")) {
            //删除设备，实时同步
            ZaSysDevice net = gateMap.get(session.getId());
            JSONObject ps = JSONObject.parseObject(req.getParam());
            deleteCamera(net.getCode(), ps.getString(JinzhiConst.FIELD_DEVICE_ID));
            log.info("删除摄像机 {} - {}", net.getCode(), ps.getString(JinzhiConst.FIELD_DEVICE_ID));
        }
        else if (req.getAction().equalsIgnoreCase("DeviceDelAllReq")) {
            //删除全部设备，实时同步
            ZaSysDevice net = gateMap.get(session.getId());
            deleteCamera(net.getCode(), null);
            log.info("删除网关全部摄像机 {}", net.getCode());
        }
        else if (req.getAction().equalsIgnoreCase("AIEventNotify")) {
            //AI事件
            handleAiEvent(session, req);
        }
        else {
            log.debug("no thing to do： {}", req.getAction());
        }
    }

    private void handleAiEvent(Session session, JinzhiWsReq req) {
        // 解析json数据
        JSONObject aiEvent = JSON.parseObject(JSON.toJSONString(req));
        JSONObject param = aiEvent.getJSONObject("param");
        JSONObject event = param.getJSONObject("event");

        // 只处理第一个target元素
        for (int i = 0; i < event.getJSONArray("targets").size(); i++) {
            JSONObject target = event.getJSONArray("targets").getJSONObject(i);

            String score = target.getString("score");


            if (Integer.parseInt(score) < AI_SCORE) {
                log.warn("当前AI事件置信度:{} , 小于告警阈值{}, 忽略该事件!" ,score, AI_SCORE);
                continue;
            } else {
                log.info("执行AI事件 , 置信度:{} !" ,score);
            }

            String deviceId = target.getString("deviceID");
            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(deviceId, gateMap.get(session.getId()).getCode());
            if(zaSysDevice == null) {
                log.warn("未注册的摄像机 {} ", deviceId);
                continue;
            }
            // 事件类型
            String eventType = target.getString("eventID");
            // 保存图片
            String alarmImg = null;
            String picID = target.getString("picID");
            JSONArray pics = event.getJSONArray("pics");
            for (int j = 0; j < pics.size(); j++) {
                JSONObject pic = pics.getJSONObject(j);
                String picBase64 = pic.getString("image");
                if (pic.getString("picID").equals(picID)) {
                    alarmImg = drawRedRect( deviceId, eventType, picBase64,
                                    target.getInteger("x"),
                                    target.getInteger("y"),
                                    target.getInteger("w"),
                                    target.getInteger("h")
                            );
                }
            }

            MqMessage.Facility facility = MqMessage.createFacility(zaSysDevice);
            MqMessage mqMessage = MqMessage.createAlarm(zaSysDevice.getId(), JinZhiHandler.PROTOCOL_NAME, facility, eventType, JSONObject.toJSONString(event));
            mqMessage.setTime(DateUtil.parseDateTime(event.getString("time")));
            mqMessage.setImageUrl(alarmImg);
            jinZhiHandler.consumeMsg(mqMessage);
        }
    }


    /**
     * 告警图片
     * @param deviceId 设备
     * @param eventType 事件类型
     * @param base64 图片BASE64
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @return String
     */
    public static String drawRedRect(String deviceId, String eventType, String base64, int x, int y, int width, int height) {
        if (StrUtil.isBlank(base64)) {
            return base64;
        }
        // 输出图路径
        String fileName =  "/jinzhi/" + deviceId+"_"+eventType + ".jpg";
        String outputPath = ZhianConfig.getProfile() + fileName;

        FileUtil.mkParentDirs(outputPath);

        // 读取图片
        BufferedImage image = null;

        try {
            image = ImageIO.read(new ByteArrayInputStream(Base64.decode(base64)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 获取图像绘图对象
        Graphics2D g2d = image.createGraphics();

        // 设置红色和线宽
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(5));

        // 画矩形
        g2d.drawRect(x, y, width, height);

        // 清理资源
        g2d.dispose();

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
    public void deleteCamera(String net, String cameraId) {
        ZaSysDevice dc = new ZaSysDevice();
        dc.setType(DeviceType.CAMERA);
        dc.setNet(net);
        ;
        if (StringUtils.isNotEmpty(cameraId)) {
            dc.setCode(cameraId);
        }
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        for (ZaSysDevice camera : list) {
            // TODO
            // jinZhiHandler.pushDevice(camera, MqMessage.DEVICE_REMOVE, null);
            deviceService.deleteZaSysDeviceById(camera.getId());
        }
    }

    /**
     * 同步设备状态
     *
     * @param camera the camera
     * @param status the status
     */
    public void checkCameraStatus(ZaSysDevice camera, String status) {
        // 默认状态为在线状态
        if (StringUtils.isEmpty(camera.getOnline())) {
            camera.setOnline("1");
        }
        // 离线转在线
        if (camera.getOnline().equalsIgnoreCase("0")
                && status.equalsIgnoreCase("online")) {
            camera.setOnline("1");
            deviceService.updateZaSysDevice(camera);
            videoHelper.pushState(camera, AlarmType.ONLINE);
        }
        // 在线转离线
        else if (camera.getOnline().equalsIgnoreCase("1")
                && !status.equalsIgnoreCase("online")) {
            camera.setOnline("0");
            deviceService.updateZaSysDevice(camera);
            videoHelper.pushState(camera, AlarmType.OFFLINE);
        }
        else if (new Random().nextInt(5) < 2) {
            //随机向上报告摄像机的状态
            videoHelper.pushState(camera, status.equalsIgnoreCase("online") ? AlarmType.ONLINE : AlarmType.OFFLINE);
        }
    }

    /**
     * 自动注册网关信息
     *
     * @param mac   the mac
     * @param model the model
     * @param ip    the ip
     * @return za sys device
     */
    public ZaSysDevice addGateway(String mac, String model, String ip) {
        IZaSysDeviceService deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
        ZaSysDevice dc = new ZaSysDevice();
        dc.setCode(mac);
        dc.setType(DeviceType.VAG);
        List<ZaSysDevice> netList = deviceService.selectZaSysDeviceList(dc);
        if (StringUtils.isNotEmpty(netList)) {
            dc = netList.get(0);

            //上报上线
            if (StringUtils.isEmpty(dc.getOnline()) || dc.getOnline().equalsIgnoreCase("0")) {
                dc.setOnline("1");
                deviceService.updateZaSysDevice(dc);
                VideoHelper videoHelper = SpringUtils.getBean(VideoHelper.class);
                videoHelper.pushState(dc, AlarmType.ONLINE);
            }

            dc.setIp(ip);
            deviceService.updateZaSysDevice(dc);

            //不结束，强制推送
            //return dc;
        } else {

            JSONObject info = new JSONObject();
            info.put("mac", mac);
            info.put("ip", ip);
            info.put("port", "8080");

            //保存网关信息
            dc.setIp(ip);
            dc.setModel(model != null ? model : JinzhiConst.MODEL_NAME);
            dc.setName(dc.getModel() + " " + ip);
            dc.setType(DeviceType.VAG);
            dc.setPfCode(JinZhiHandler.PLATFORM_NAME);
            dc.setCode(mac);
            dc.setOnline("1");
            dc.setWireless("0");
            dc.setRemark(info.toJSONString());
            deviceService.insertZaSysDevice(dc);
        }

        // TODO推送到平台
        // jinZhiHandler.pushDevice(dc, MqMessage.DEVICE_ADD, dc.getRemark());
        return dc;
    }

    /**
     * 同步摄像机
     *
     * @param net    the net
     * @param camera {"deviceID":"2CB165135C7F517647732B0430B869CF","name":"65","type":"ONVIF","ip":"192.168.1.65","username":"admin","password":"zhian12345","streamUrl":"rtsp://admin:zhian12345@192.168.1.65:554/Streaming/Channels/101?transportmode=unicast&profile=Profile_1","streamSubUrl":"rtsp://admin:zhian12345@192.168.1.65:554/Streaming/Channels/102?transportmode=unicast&profile=Profile_2","longitude":"","latitude":"","address":"","modifyTime":"2024/07/01 13:52:54","exid1400":"","exidgb":"32412314324","scenes":"","status":"online","resolution":"","fps":-1,"videoCode":"","audioCode":""
     * @return the za sys device
     */
    public ZaSysDevice addCamera(ZaSysDevice net, JSONObject camera) {
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
        }

        String remark = JSONObject.toJSONString(device);
        String deviceId = camera.getString(JinzhiConst.FIELD_DEVICE_ID);
        String online = camera.getString(JinzhiConst.FIELD_STATUS).equalsIgnoreCase(JinzhiConst.ONLINE) ? "1" : "0";
        //先根据deviceID和网关，查询设备
        ZaSysDevice dc = deviceService.selectZaSysDeviceByCode(deviceId, net.getCode());
        if (dc != null) {
            //如果设备已存在，更新remark扩展信息
            if (StringUtils.isEmpty(dc.getRemark()) || !dc.getRemark().equalsIgnoreCase(remark)) {
                dc.setRemark(remark);
                deviceService.updateZaSysDevice(dc);
            }
            if(StringUtils.isEmpty(dc.getOnline()) || !dc.getOnline().equalsIgnoreCase(online)){
                dc.setOnline(online);
                deviceService.updateZaSysDevice(dc);
            }
            //不结束，强制推送
            //return dc;
        } else {
            //保存新设备信息
            dc = new ZaSysDevice();
            dc.setCode(deviceId);
            dc.setNet(net.getCode());
            dc.setType(DeviceType.CAMERA);
            dc.setOnline("1");
            dc.setWireless("0");
            dc.setName(camera.getString(JinzhiConst.FIELD_NAME));
            dc.setPfCode(JinZhiHandler.PLATFORM_NAME);
            dc.setIp(camera.getString(JinzhiConst.FIELD_IP));
            dc.setOnline(online);
            dc.setRemark(remark);
            deviceService.insertZaSysDevice(dc);
        }

        // TODO推送到平台
        // jinZhiHandler.pushDevice(dc, MqMessage.DEVICE_ADD, camera.toJSONString());
        return dc;
    }

    /**
     * 响应错误信息
     *
     * @param session the session
     * @param action  the action
     * @param param   the param
     */
    public void sendError(Session session, String action, JSONObject param) {
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
    public void sendSuccess(Session session, String action, JSONObject param) {
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

    /**
     * 主动发送消息
     *
     * @param session the session
     * @param content the content
     */
    public static void sendMsg(Session session, String content) {
        try {
            log.debug("response success to jinzhi ws: {}", content);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(content.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
