package com.zhian.gateway.framework.web.websocket;

import com.zhian.gateway.common.core.domain.entity.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息工具类
 *
 * @author ruoyi
 */
public class WebSocketUtils {
    /**
     * SemaphoreUtils 日志控制器
     */
    private static final Logger log = LoggerFactory.getLogger(WebSocketUtils.class);

    /**
     * 用户集
     */
    private static Map<String, Long> USERS = new ConcurrentHashMap<String, Long>();
    /**
     * 用户集
     */
    private static Map<String, Session> SESSIONS = new ConcurrentHashMap<String, Session>();

    /**
     * 用户集
     */
    private static Map<String, Set<String>> SUBSCRIBES = new ConcurrentHashMap<>();

    /**
     * 新连接一个用户
     *
     * @param session
     * @param user
     */
    public static void addUser(Session session, SysUser user) {
        addUser(session, user.getUserId());
    }

    /**
     * 添加一个已连接的用户
     *
     * @param session session
     * @param userId userId
     */
    public static void addUser(Session session, Long userId) {
        USERS.put(session.getId(), userId);
        SESSIONS.put(session.getId(), session);
    }

    /**
     * 订阅消息
     *
     * @param sessionId
     * @param subs
     */
    public static void subscribe(String sessionId, Set<String> subs) {
        SUBSCRIBES.put(sessionId, subs);
    }

    /**
     * 取消订阅
     *
     * @param sessionId
     * @param subs
     */
    public static void unsubscribe(String sessionId, Set<String> subs) {
        Set<String> userSub = SUBSCRIBES.get(sessionId);
        if (userSub == null || userSub.isEmpty()) {
            return;
        }
        for (String s : subs) {
            userSub.remove(s);
        }
        SUBSCRIBES.put(sessionId, userSub);
    }

    /**
     * 查询用户的连接信息
     *
     * @param sessionId
     */
    public static Long getUser(String sessionId) {
        return USERS.get(sessionId);
    }

    /**
     * 断开一个用户连接
     *
     * @param sessionId
     */
    public static void removeUser(String sessionId) {
        USERS.remove(sessionId);
        SESSIONS.remove(sessionId);
        SUBSCRIBES.remove(sessionId);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId
     * @param msg
     */
    public static void sendToUser(Long userId, String msg) {
        for (String sessionId : USERS.keySet()) {
            log.debug("send ws msg to {}: {}", userId, msg);
            if (!USERS.get(sessionId).equals(userId)) continue;
            sendMsg(SESSIONS.get(sessionId), msg);
        }
    }

    /**
     * 向所有人发送消息
     *
     * @param msg
     */
    public static void sendToAll(String msg) {
        for (Session session : SESSIONS.values()) {
            sendMsg(session, msg);
        }
    }

    /**
     * 发送消息的方法
     *
     * @param session
     * @param msg     WebSocketMsg
     */
    private static void sendMsg(Session session, String msg) {

        try {
            synchronized (session) {
                session.getBasicRemote().sendText(msg.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("向前端推送消息失败", e);
        }
    }
}
