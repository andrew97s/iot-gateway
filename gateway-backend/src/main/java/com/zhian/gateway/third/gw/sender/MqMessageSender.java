package com.zhian.gateway.third.gw.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.third.utils.RabbitMqUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * MQ消息发送器
 *
 * @author tongwenjin
 * @since 2024/8/13
 */

@Slf4j
public class MqMessageSender implements MessageSender {

    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "gateway";
    /**
     * The constant QUEUE_NAME.
     */
    public static String QUEUE_NAME = "za_monitor";
    /**
     * The constant QUEUE_EXCHANGE.
     */
    public static String QUEUE_EXCHANGE = "za";
    /**
     * The constant QUEUE_KEY.
     */
    public static String QUEUE_KEY = "za";
    /**
     * The constant channel.
     */
    private Channel channel;
    /**
     * The constant platform.
     */
    private ZaSysPlatform platform;

    @Override
    public boolean start(ZaSysPlatform platform) {
        this.platform = platform;
        IZaSysErrorService errorService = SpringUtils.getBean(IZaSysErrorService.class);
        // 初始化MQ连接
        String ip = platform.getConfigStr("ip");
        // 为防止dev 与 test 环境互相影响 ， 此处应通过 profiles 来区分 vhost
        String vhost = Arrays.asList(SpringUtils.getActiveProfiles()).contains("test") ?
                "test" : platform.getConfigStr("vhost");
        Connection connection = RabbitMqUtil.getConnection(
                platform.getConfigStr("ip"),
                platform.getConfigInt("port"),
                vhost,
                platform.getConfigStr("username"),
                platform.getConfigStr("password")
        );
        if (connection == null) {
            log.error("连接RabbitMQ失败");
            errorService.log(ZaSysError.TYPE_API_ERROR, "连接RabbitMQ异常", RabbitMqUtil.getException().getMessage(), platform.getConfig());
            throw new IllegalArgumentException("网关初始化失败,创建MQ连接为空!");
        }

        //消息有效期是30天
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 1000 * 3600 * 24 * 30L);

        ShutdownListener shutdownListener =
                (ShutdownSignalException cause) -> log.error("RabbitMQ {} 连接异常断开: {}", ip, cause.getMessage());
        QUEUE_EXCHANGE = platform.getConfigStr("exchange");
        QUEUE_NAME = platform.getConfigStr("queue");
        QUEUE_KEY = platform.getConfigStr("key");
        channel = RabbitMqUtil.produce(connection, QUEUE_EXCHANGE, QUEUE_NAME, QUEUE_KEY, args, shutdownListener);
        if (channel == null) {
            log.error("创建RabbitMQ生产队列失败");
            errorService.log(ZaSysError.TYPE_API_ERROR, "创建RabbitMQ生产队列失败", RabbitMqUtil.getException().getMessage(), platform.getConfig());
            throw new IllegalArgumentException("网关初始化失败,创建MQ通道为空!");
        }
        return false;
    }

    @Override
    public boolean stop() {
        RabbitMqUtil.close(platform.getConfigStr("ip"));
        channel = null;
        return true;
    }

    @Override
    public boolean isAlive() {
        return channel != null && channel.isOpen();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public void send(String message) {
        log.debug("publish msg to rabbit: {}", message);
        try {
            if (channel != null) {
                channel.basicPublish(QUEUE_EXCHANGE, QUEUE_KEY, null, message.getBytes(StandardCharsets.UTF_8));
            } else {
                log.error("未获取到RabbitMQ的连接,无法处理消息");
            }
        } catch (Exception e) {
            e.printStackTrace();
            SpringUtils.getBean(IZaSysErrorService.class).log(ZaSysError.TYPE_MQ, "RabbitMQ发送消息失败", e.getMessage(), message);

        }
    }
}
