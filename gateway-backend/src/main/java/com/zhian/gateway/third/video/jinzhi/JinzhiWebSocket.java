package com.zhian.gateway.third.video.jinzhi;

import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.common.utils.ExceptionUtil;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.framework.web.websocket.AbstractWebSocket;
import com.zhian.gateway.framework.web.websocket.config.WebSocketSessionConfigurator;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.system.service.ISysConfigService;
import com.zhian.gateway.third.video.VideoHelper;
import com.zhian.gateway.third.video.jinzhi.bo.JzMessage;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    public static Integer AI_SCORE = 60;

    /**
     * The constant KEY_MAP.
     */
    public static final Map<String, JinzhiKey> KEY_MAP = new ConcurrentHashMap<>();
    /**
     * The constant GATE_MAP.
     */
    public static final Map<String, ZaSysDevice> GATE_MAP = new ConcurrentHashMap<>();
    /**
     * 同一 session 的消息串行链，避免 offline/online 等状态消息乱序覆盖
     */
    private static final ConcurrentHashMap<String, CompletableFuture<Void>> SESSION_MSG_CHAIN = new ConcurrentHashMap<>();
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
        if (StrUtil.isNotBlank(score) && StrUtil.isNumeric(score)) {
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
        KEY_MAP.put(session.getId(), key);
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
        ZaSysDevice gateway = GATE_MAP.get(session.getId());
        if (gateway != null) {
            jinZhiHandler.removeGateway(gateway);
        }
        KEY_MAP.remove(session.getId());
        GATE_MAP.remove(session.getId());
        SESSION_MSG_CHAIN.remove(session.getId());
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
        log.info("====receive: {} ", StrUtil.maxLength(message, 1024));
        try {
            // 视频网关经常会出现 极短事件内连续上报两条设备状态（一条离线、一条在线）-会导致设备状态同步异常的问题
            // 同 session 串行异步处理，保证先离线后在线等消息按到达顺序生效
            JinZhiHandler handler = SpringUtils.getBean(JinZhiHandler.class);
            submitOrdered(session.getId(), () -> handler.processMsg(new JzMessage(session, message)));
        } catch (Exception e) {
            log.error("处理视频网关消息时异常： {}", e.getMessage());
        }
    }

    /**
     * 按 session 串行提交任务，前一条失败不影响后续执行
     */
    private void submitOrdered(String sessionId, Runnable task) {
        SESSION_MSG_CHAIN.compute(sessionId, (id, prev) -> {
            CompletableFuture<Void> base = prev != null ? prev : CompletableFuture.completedFuture(null);
            return base.handle((r, ex) -> null)
                    .thenRunAsync(task)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.error("处理金智消息失败:{}", ex.getMessage());
                            ex.printStackTrace();
                        }
                    });
        });
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
