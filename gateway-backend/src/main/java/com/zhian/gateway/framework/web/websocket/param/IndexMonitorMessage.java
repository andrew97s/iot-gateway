package com.zhian.gateway.framework.web.websocket.param;

import lombok.Data;

import java.util.Set;

@Data
public class IndexMonitorMessage {
    public static final int SUBSCRIBE = 1000;
    public static final int UNSUBSCRIBE = 1001;

    /**
     * 类型 2000订阅 2001取消订阅
     */
    private Integer type;

    /**
     * 订阅的消息类型
     */
    private Set<String> msgTypes;
}
