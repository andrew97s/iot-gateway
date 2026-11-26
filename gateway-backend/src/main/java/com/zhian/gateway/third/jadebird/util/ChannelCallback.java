package com.zhian.gateway.third.jadebird.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

import java.io.IOException;

/**
 * MQ通道回调
 *
 * @author tongwenjin
 * @since 2026/8/21
 */
public interface ChannelCallback {

    void handle(Channel channel, Delivery message) throws IOException;
}
