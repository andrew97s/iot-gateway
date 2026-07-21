package com.zhian.gateway.third.video.roc;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.video.roc.common.RocConstants;
import com.zhian.gateway.third.video.roc.common.RocContext;
import com.zhian.gateway.third.video.roc.common.RocMsg;
import com.zhian.gateway.third.video.roc.event.RocFaceEvent;
import com.zhian.gateway.third.video.roc.ws.RocWebSocketEndpoint;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@Component
@Slf4j
public class RocHandler extends BasePlatformHandler {

    public static final String PLATFORM_NAME = "roc";
    public static final String PROTOCOL_NAME = "roc";
    private static String TARGET_WS_URL = "ws://192.168.1.88"; // 目标 WebSocket 服务器地址
    public static ZaSysPlatform zaSysPlatform;
    private static Boolean running = false;
    public static Session commSession;

    @Override
    public boolean start(ZaSysPlatform platform) {
        log.info("启动人脸识别服务");
        RocHandler.zaSysPlatform = platform;
        if (StringUtils.isNotEmpty(platform.getIp())) {
            TARGET_WS_URL = "ws://" + platform.getIp() + (platform.getPort() == null ? "" : (":" + platform.getPort()));
        }
        connectToServer();
        running = (commSession != null);
        if (running) {
            pushDevice();
        }
        return running;
    }

    @Override
    public boolean stop() {
        log.info("停止人脸识别服务");
        closeServerSession();
        running = false;
        return true;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        if (running) {
            pushDevice();
            if (!RocContext.context.getFaceEnable()) {
                RocMsg msg = RocMsg.create(RocConstants.METHOD_GET, RocConstants.URI_FACE_RULE);
                sendToServer(msg);
            }
        }
        return DeviceSyncInfo.success(1);
    }

    /**
     * 推送人脸识别设备到业务平台
     */
    public void pushDevice() {
        if (RocContext.getInstance().getDevice() != null
                || StringUtils.isEmpty(RocContext.getInstance().getSerialNum())) {
            return;
        }

        ZaSysDevice sysDevice = deviceService.selectByCode(RocContext.getInstance().getSerialNum(), getPlatform());
        if (sysDevice == null) {
            sysDevice = new ZaSysDevice();
            sysDevice.setType(PLATFORM_NAME);
            sysDevice.setOnline("1");
            sysDevice.setNet(PLATFORM_NAME);
            sysDevice.setIp(zaSysPlatform.getIp());
            sysDevice.setCode(RocContext.getInstance().getSerialNum());
            sysDevice.setName(RocContext.context.getDeviceName());
            sysDevice.setPfCode(PLATFORM_NAME);
            sysDevice.setWireless("0");
            deviceService.insertZaSysDevice(sysDevice);
            DeviceUtil.pushDevice(DeviceUpdReq.newAddReq(sysDevice, null));
        } else if (!sysDevice.getIp().equalsIgnoreCase(zaSysPlatform.getIp())) {
            sysDevice.setIp(zaSysPlatform.getIp());
            deviceService.updateZaSysDevice(sysDevice);
            DeviceUtil.pushDevice(DeviceUpdReq.newUpdReq(sysDevice, null));
        }

        RocContext.getInstance().setDevice(sysDevice);
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
     * 提供子类实现msgObj自动类型转换
     *
     * @param msgObj the msg obj
     */
    public ProcessInfo doProcessMsg(Object msgObj) {
        ZaSysDevice device = RocContext.getInstance().getDevice();
        if (device == null) {
            log.error("未同步人脸摄像机设备，忽略人脸识别事件：{}", msgObj);
            return null;
        }
        if (msgObj instanceof RocFaceEvent) {
            MqMessage msg = genAlarm(device, RocConstants.EVENT_FACE_COMPARE, JSONObject.toJSONString(msgObj));
            return ProcessInfo.newInstance(
                    device, JSON.toJSONString(msgObj), MsgConstants.MSG_TYPE_ALARM, Collections.singletonList(msg)
            );
        } else {
            log.error("暂不支持的消息内容");
        }
        return null;
    }

    @Override
    public R doControl(ControlVo controlVo) {
        //流地址自动拼接
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            return R.success(TARGET_WS_URL);
        } else if (ControlVo.CMD_SET.equalsIgnoreCase(controlVo.getCommand())) {
            //配置，主要是人脸
            RocFaceEvent faceEvent = JSONObject.parseObject(controlVo.getValue(), RocFaceEvent.class);
            addFace(faceEvent);
            return R.success();
        } else {
            return R.error("不支持的操作");
        }
    }

    private void addFace(RocFaceEvent faceEvent) {
        if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.CLEAR)) {
            RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_DELETE);
            JSONArray list = new JSONArray();
            for (String user : RocContext.getInstance().getFaceSet()) {
                JSONObject u = new JSONObject();
                u.put("name", user);
                list.add(u);
            }
            JSONObject body = new JSONObject();
            body.put("groupName", RocConstants.GROUP_NAME);
            body.put("groupList", list);
            msg.setBody(Collections.singletonMap("FRDeleteDescription", body));
            sendToServer(msg);
            return;
        }

        if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.DELETE)) {
            RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_DELETE);
            JSONObject body = new JSONObject();
            body.put("groupName", RocConstants.GROUP_NAME);
            body.put("groupList", new String[]{faceEvent.getName()});
            msg.setBody(Collections.singletonMap("FRDeleteDescription", body));
            sendToServer(msg);
            return;
        }

        if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.ADD) && RocContext.getInstance().getFaceSet().contains(faceEvent.getName())) {
            faceEvent.setAction(RocFaceEvent.MODIFY);
        } else if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.MODIFY) && !RocContext.getInstance().getFaceSet().contains(faceEvent.getName())) {
            faceEvent.setAction(RocFaceEvent.ADD);
        }
        if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.ADD)) {
            faceEvent.setAction(RocFaceEvent.MODIFY);
            RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_ADD);
            JSONObject body = new JSONObject();
            body.put("groupName", RocConstants.GROUP_NAME);
            body.put("name", faceEvent.getName());
            body.put("describe", faceEvent.getDescribe());
            body.put("faceData", faceEvent.getFaceImg());
            msg.setBody(Collections.singletonMap("FRAddDescription", body));
            sendToServer(msg);
            RocContext.getInstance().getFaceSet().add(faceEvent.getName());
        } else if (faceEvent.getAction().equalsIgnoreCase(RocFaceEvent.MODIFY)) {
            RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_MODIFY);
            JSONObject body = new JSONObject();
            body.put("groupName", RocConstants.GROUP_NAME);
            body.put("name", faceEvent.getName());
            body.put("newName", faceEvent.getName());
            body.put("describe", faceEvent.getDescribe());
            body.put("needModifyFacePic", true);
            body.put("faceData", faceEvent.getFaceImg());
            msg.setBody(Collections.singletonMap("FRAddDescription", body));
            sendToServer(msg);
        }
    }


    /**
     * 连接人脸识别摄像机
     */
    private void connectToServer() {
        try {
            // 等待目标 WebSocket 连接建立
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            // 设置缓冲区大小（例如 256 KB）
            container.setDefaultMaxBinaryMessageBufferSize(256 * 1024); // 二进制消息缓冲区
            container.setDefaultMaxTextMessageBufferSize(256 * 1024);   // 文本消息缓冲区
            // 创建自定义请求头配置
            ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
                @Override
                public void beforeRequest(Map<String, List<String>> headers) {
                    headers.put("Sec-WebSocket-Protocol", Collections.singletonList("rocapi"));
                }
            };
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .configurator(configurator)
                    .build();
            commSession = container.connectToServer(new RocWebSocketEndpoint(), config, new URI(TARGET_WS_URL));
            // 连接建立后，发送登录请求
            log.info("成功连接至目标ROC服务({})!", TARGET_WS_URL);
        } catch (Exception e) {
            e.printStackTrace();
            commSession = null;
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "连接人脸识别摄像机失败", e.getMessage(), TARGET_WS_URL);
        }
    }

    /**
     * 向摄像机发消息
     *
     * @param message
     */
    public void sendToServer(RocMsg message) {
        if (commSession != null && commSession.isOpen()) {
            try {
                // 设置授权信息
                if (!message.getUri().equalsIgnoreCase(RocConstants.URI_SESSION)) {
                    doAuth(message);
                }
                log.info("Send to ROC server : {}", message.toJsonStr());
                // 发送消息
                commSession.getBasicRemote().sendText(message.toJsonStr());
            } catch (Exception e) {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "向人脸识别摄像机发送消息失败", e.getMessage(), message.toJsonStr());
                e.printStackTrace();
            }
        }
    }

    private void closeServerSession() {
        try {
            if (commSession != null) {
                commSession.close();
                commSession = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 认证
     *
     * @param message
     */
    private void doAuth(RocMsg message) {
        RocContext context = RocContext.getInstance();
        // 设置授权信息
        RocMsg.Auth auth = new RocMsg.Auth();
        auth.setQop(context.getQop());
        auth.setAlgorithm("MD5");
        auth.setNonce(context.getNonce());
        auth.setCnonce(context.getCnonce());
        auth.setRealm(context.getRealm());
        auth.setUsername(context.getUsername());
        auth.setNc((context.getSequence() + 1) + "");

        // 计算 HA1 = MD5(username:realm:password)
        String ha1 = DigestUtil.md5Hex(
                context.getUsername() + ":" + context.getRealm() + ":" + context.getPassword()
        );
        // 计算 HA2 = MD5(method:uri)
        String ha2 = DigestUtil.md5Hex(message.getMethod() + ":" + message.getUri());

        // 计算 response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
        String response = DigestUtil.md5Hex(
                ha1 + ":" + context.getNonce() + ":" + (context.getSequence() + 1) + ":" +
                        context.getCnonce() + ":" + context.getQop() + ":" + ha2
        );
        auth.setResponse(response);
        message.setAuth(auth);
    }

    @Override
    public List<Map<String, Object>> getConfigSchema() {
        java.util.List<Map<String, Object>> schema = new ArrayList<>();

        Map<String, Object> password = new LinkedHashMap<>();
        password.put("code", "password");
        password.put("name", "密码");
        password.put("desc", "密码");
        password.put("type", "text");
        password.put("required", true);
        password.put("defaultValue", "123456");
        schema.add(password);

        Map<String, Object> username = new LinkedHashMap<>();
        username.put("code", "username");
        username.put("name", "账号");
        username.put("desc", "账号");
        username.put("type", "text");
        username.put("required", true);
        username.put("defaultValue", "admin");
        schema.add(username);

        return schema;
    }
}
