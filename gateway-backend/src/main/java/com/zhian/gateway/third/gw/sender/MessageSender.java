package com.zhian.gateway.third.gw.sender;

import com.zhian.gateway.sys.domain.ZaSysPlatform;

/**
 * 消息推送服务
 *
 * @author tongwenjin
 * @since 2024 /8/13
 */
public interface MessageSender {

    /**
     * start
     *
     * @param platform the za sys platform
     * @return the boolean
     */
    boolean start(ZaSysPlatform platform);

    /**
     * stop
     *
     * @return the boolean
     */
    boolean stop();

    /**
     * 探活
     *
     * @return boolean
     */
    boolean isAlive();

    /**
     * Send.
     *
     * @param message the message
     */
    void send(String message);
}
