package com.zhian.gateway.core.message.builder;

import com.zhian.gateway.core.message.Message;

/**
 * 全息网关消息builder
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
public class MessageBuilder {

    public static Message buildDevice(){
        return new Message();
    }

    public static Message buildAlarm(){
        return new Message();
    }

    public static Message buildTelemetry(){
        return new Message();
    }
}
