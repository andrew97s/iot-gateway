package com.zhian.gateway.framework.web.websocket.endpoint;

import com.zhian.gateway.common.utils.ExceptionUtil;
import com.zhian.gateway.common.utils.JsonMapperUtils;
import com.zhian.gateway.framework.web.websocket.AbstractWebSocket;
import com.zhian.gateway.framework.web.websocket.WebSocketUtils;
import com.zhian.gateway.framework.web.websocket.config.WebSocketSessionConfigurator;
import com.zhian.gateway.framework.web.websocket.param.IndexMonitorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 与前端调度台建立起的websocket连接
 * 客户端连接 ws://192.168.1.110:9999/ws/420200
 *
 * 此class 每个ws连接对应一个独立的实例
 *
 * @author zhian
 */
@ServerEndpoint(value = "/ws/{userId}", configurator = WebSocketSessionConfigurator.class)
@Component
public class IndexDispatchWebSocket extends AbstractWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(IndexDispatchWebSocket.class);

    /**
     * On open.
     *
     * @param session the session
     * @param userId  the user id
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config,
                       @PathParam("userId") Long userId) {

        try {
            if (userId == null) {
                session.getBasicRemote().sendText("连接参数缺少用户参数,会话被服务端中断");
                session.close();
                return;
            } else if (userId.intValue() == 0) {
                logger.info("级联网关消息");
            }
            WebSocketUtils.addUser(session, userId);
            // 在线数加1
            calcOnlineCount(1);

            logger.info("有新连接加入{}！当前在线人数为{}", getClientIpAddress(config), getOnlineCount());
        } catch (IOException e) {
            logger.error("websocket IO异常");
        }
    }

    /**
     * On close.
     *
     * @param session the session
     * @throws IOException the io exception
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        WebSocketUtils.removeUser(session.getId());
        //在线数减1
        calcOnlineCount(-1);
        logger.info(
                "有一连接关闭！当前在线人数为{}",
                getOnlineCount()
        );
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
                logger.error("session 连接发生异常,主动关闭连接!,异常信息:{}", ExceptionUtil.getExceptionMessage(t));
                this.onClose(session);
            } else {
                logger.error("Ws error occurred , msg : {}", ExceptionUtil.getExceptionMessage(t));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to close ws connection , msg :{}", ExceptionUtil.getExceptionMessage(e));
        }
    }

    /**
     * On message.
     *
     * @param session the session
     * @param message the message
     */
    @OnMessage
    public void onMessage(Session session, String message) {

        logger.info("====receive: " + message);
        // 收到心跳消息,并回复一个心跳包到客户端
        if ("{}".equals(message) || "{heartBeat:1}".equals(message)) {
            try {
                session.getBasicRemote().sendText("{\"heartBeat\":\"ok\",\"systime\":" + System.currentTimeMillis() + "}");
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("回复前端的心跳发生异常:{}", ExceptionUtil.getExceptionMessage(e));
            }
            return;
        }

        // 处理前端订阅的消息
        IndexMonitorMessage monitorMessage = JsonMapperUtils.defaultMapper().fromJson(message, IndexMonitorMessage.class);
        if (monitorMessage.getType() == IndexMonitorMessage.SUBSCRIBE) {
            WebSocketUtils.subscribe(session.getId(), monitorMessage.getMsgTypes());
        } else if (monitorMessage.getType() == IndexMonitorMessage.UNSUBSCRIBE) {
            WebSocketUtils.unsubscribe(session.getId(), monitorMessage.getMsgTypes());
        }
        logger.info("当前socket通道订阅信息：{}", monitorMessage);

    }

    /**
     * 服务端定时向客户端发送消息，保持心跳 ? 心跳需要超时机制 - 超时断开连接
     */
    @PostConstruct
    public void keepHeart() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                WebSocketUtils.sendToAll("{\"heart\":\"auto\",\"systime\":" + System.currentTimeMillis() + "}");
            }
        }, 30L, 40L, TimeUnit.SECONDS);

    }
}
