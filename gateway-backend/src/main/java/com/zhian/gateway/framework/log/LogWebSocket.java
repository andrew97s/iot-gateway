package com.zhian.gateway.framework.log;

import com.zhian.gateway.framework.web.websocket.config.WebSocketSessionConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实时日志连接。浏览器通过 /ws/log?token= 接入，收到的是与日志文件相同的文本行。
 */
@Component
@ServerEndpoint(value = "/ws/log", configurator = WebSocketSessionConfigurator.class)
public class LogWebSocket
{
    private static final Logger logger = LoggerFactory.getLogger(LogWebSocket.class);

    private static final AtomicInteger IDS = new AtomicInteger();

    private String id;

    @OnOpen
    public void onOpen(Session session)
    {
        id = "id-" + IDS.incrementAndGet();
        session.setMaxIdleTimeout(0);
        LogStreamHub.register(id, session);
        logger.info("[WebLog] 连接已建立: {}", id);
    }

    @OnClose
    public void onClose()
    {
        LogStreamHub.unregister(id);
        logger.info("[WebLog] 连接已关闭: {}", id);
    }

    @OnError
    public void onError(Throwable error)
    {
        LogStreamHub.unregister(id);
    }
}