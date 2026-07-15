package com.zhian.gateway.third.video.roc.ws;

import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.third.video.roc.RocHandler;
import com.zhian.gateway.third.video.roc.common.RocConstants;
import com.zhian.gateway.third.video.roc.common.RocMsg;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.*;

/**
 * ROC的websocket消息处理
 */
@Slf4j
public class RocWebSocketEndpoint extends Endpoint {
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        RocHandler.commSession = session;

        // 监听目标服务器消息，并转发给客户端
        for (MessageHandler messageHandler : session.getMessageHandlers()) {
            session.removeMessageHandler(messageHandler);
        }
        session.addMessageHandler(String.class, new RocMessageHandler());

        //连接成功后，自动登录
        RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_SESSION);
        RocMsg.Auth auth = new RocMsg.Auth();
        auth.setUsername("admin");
        msg.setAuth(auth);
        SpringUtils.getBean(RocHandler.class).sendToServer(msg);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        log.info("roc ws 服务器连接关闭:{}", closeReason.getReasonPhrase());
        RocHandler.commSession = null;
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        log.error("roc ws error : {}", throwable.getMessage());
    }
}
