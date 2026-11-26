package com.zhian.gateway.third.cascade;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.utils.ExceptionUtil;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.framework.web.websocket.AbstractWebSocket;
import com.zhian.gateway.framework.web.websocket.config.WebSocketSessionConfigurator;
import com.zhian.gateway.sys.domain.ZaSysCascade;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysCascadeService;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 与前端调度台建立起的websocket连接
 * 客户端连接 ws://192.168.1.110:9999/cascade/420200
 *
 * 此class 每个ws连接对应一个独立的实例
 *
 * @author zhian
 */
@ServerEndpoint(value = "/cascade/{clientId}", configurator = WebSocketSessionConfigurator.class)
@Component
@Slf4j
public class CascadeServerSocket extends AbstractWebSocket {

    /** 静态变量 */
    private final static ConcurrentHashMap<String, Session> clientMap = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Long> cascadeMap = new ConcurrentHashMap<>();

    private static IZaSysCascadeService zaSysCascadeService;

    @Autowired
    public void setZaSysCascadeService(IZaSysCascadeService zaSysCascadeService){
        this.zaSysCascadeService = zaSysCascadeService;
    }

    /**
     * On open.
     *
     * @param session the session
     * @param clientId  the user id
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config,
                       @PathParam("clientId") String clientId) {
        try {
            String ip = getClientIpAddress(config);
            ZaSysCascade zaSysCascade = zaSysCascadeService.selectZaSysCascadeByCode(clientId);
            if(zaSysCascade == null){
                log.error("未注册的级联平台{} - {}", clientId, ip);
                return;
            }

            //更新在线状态
            zaSysCascade.setOnline("Y");
            if(StringUtils.isEmpty(zaSysCascade.getIp()) || !zaSysCascade.getIp().equalsIgnoreCase(ip)){
                zaSysCascade.setIp(ip);
            }
            zaSysCascadeService.updateZaSysCascade(zaSysCascade);

            clientMap.put(clientId, session);
            cascadeMap.put(session.getId(), zaSysCascade.getId());

            log.info("级联平台{}已连接！当前在线{}个", zaSysCascade.getName(), clientMap.size());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("websocket IO异常");
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
        Long cascadeId = cascadeMap.remove(session.getId());
        if(cascadeId == null){
            return;
        }

        ZaSysCascade zaSysCascade = zaSysCascadeService.selectZaSysCascadeById(cascadeId);
        zaSysCascade.setOnline("N");
        zaSysCascadeService.updateZaSysCascade(zaSysCascade);

        cascadeMap.remove(session.getId());
        clientMap.remove(zaSysCascade.getCode());
        log.info("级联平台{}已断开,当前在线{}个！", zaSysCascade.getName(), clientMap.size());
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
                this.onClose(session);
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
     * @param message the message
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("receive cascade msg: {}", message);

        Long cascadeId = cascadeMap.remove(session.getId());
        if(cascadeId == null){
            return;
        }

        MqMessage mqMessage = JSONObject.parseObject(message, MqMessage.class);
        CascadeServerHandler cascadeServerHandler = SpringUtils.getBean(CascadeServerHandler.class);
        if(mqMessage.getFacility() != null) {
            ZaSysDevice zaSysDevice = cascadeServerHandler.syncCascadeDevice(mqMessage.getFacility(), mqMessage.getDeviceId(), cascadeId);
        }
        cascadeServerHandler.processMsg(message);

    }

    /**
     * 向指定下级发送消息
     * @param clientId
     * @param msg
     */
    public static void sendTo(String clientId, String msg){
        try{
            Session session = clientMap.get(clientId);
            if(session != null){
                session.getBasicRemote().sendText(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 向所有下级发送消息
     * @param msg
     */
    public static void sendAll(String msg){
        try{
            for(Session session:clientMap.values()){
                session.getBasicRemote().sendText(msg);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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
                sendAll("{\"heart\":\"auto\",\"systime\":" + System.currentTimeMillis() + "}");
            }
        }, 1L, 1L, TimeUnit.MINUTES);

    }
}
