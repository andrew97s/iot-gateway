package com.zhian.gateway.third.jadebird.util;

import com.rabbitmq.client.*;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.utils.RabbitMqUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 青鸟MQ相关工具类
 *
 * @author tongwenjin
 * @since 2026/8/21
 */
@Slf4j
public class JbMqUtil {

    private static Channel messsageChannel;
    private static Channel heartChannel;
    private static String mqConnectionKey;

    public static void subscribeMessage(ChannelCallback callback, ZaSysPlatform platform) {
        // 关闭原有通道
        RabbitMqUtil.closeQuietly(messsageChannel);
        messsageChannel = null;

        // 重新订阅 , 交换机类型为FANOUT , 队列名称不重要
        messsageChannel = subscribe(callback, platform, "monitor.src.upload", "za-message");
    }

    public static void subscribeHeart(ChannelCallback callback, ZaSysPlatform platform) {
        RabbitMqUtil.closeQuietly(heartChannel);
        heartChannel = null;
        // 重新订阅 , 交换机类型为FANOUT , 队列名称不重要
        heartChannel = subscribe(callback, platform, "monitor.src.link", "za-heart");
    }

    public static void closeChannel() {
        RabbitMqUtil.closeQuietly(heartChannel);
        heartChannel = null;
        RabbitMqUtil.closeQuietly(messsageChannel);
        messsageChannel = null;
    }

    private static Channel subscribe(ChannelCallback callback, ZaSysPlatform platform, String exchange, String queue) {
        // 连接参数
        String ip = platform.getConfigStr("ip");
        Integer port = platform.getConfigInt("port");
        String vhost = RabbitMqUtil.resolveVhost(platform.getConfigStr("vhost"), SpringUtils.getActiveProfiles());
        String username = platform.getConfigStr("username");
        String password = platform.getConfigStr("password");

        mqConnectionKey = RabbitMqUtil.connectionKey(ip, port, vhost, username);

        Connection connection = RabbitMqUtil.getConnection(ip, port, vhost, username, password);
        if (connection == null) {
            log.error("连接RabbitMQ失败");
            return null;
        }

        CancelCallback cancelCallback = tag -> log.info("rabbit {} consumer {} cancel", platform.getName(), tag);
        Channel channel = null;
        try {
            channel = connection.createChannel();
        } catch (Exception e) {
            log.error("创建RabbitMQ channel失败, queue={}: {}", queue, e.getMessage(), e);
            return null;
        }
        channel.addShutdownListener(cause -> {
            if (cause.isInitiatedByApplication()) {
                return;
            }
            log.error("RabbitMQ {} 通道异常断开: {}", ip, cause.getReason());
        });


        Channel finalChannel = channel;
        boolean consumed = RabbitMqUtil.consume(
                channel,
                exchange,
                queue,
                "",
                (tag, msg) -> callback.handle(finalChannel, msg),
                cancelCallback,
                50
        );

        if (!consumed) {
            log.error("MQ订阅失败...");
            RabbitMqUtil.closeQuietly(channel);
            return null;
        }

        return channel;
    }
}
