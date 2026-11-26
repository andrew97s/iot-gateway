package com.zhian.gateway.third.gw.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownListener;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.third.utils.RabbitMqUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
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
     * 队列名（实例级，支持多上级平台各自独立的 MQ 配置）
     */
    private String queueName = "za_monitor";
    /**
     * 交换机
     */
    private String queueExchange = "za";
    /**
     * 路由键
     */
    private String queueKey = "za";
    /**
     * The constant channel.
     */
    private Channel channel;
    /**
     * The constant platform.
     */
    private ZaSysPlatform platform;
    private String mqConnectionKey;

    @Override
    public boolean start(ZaSysPlatform platform) {
        this.platform = platform;
        IZaSysErrorService errorService = SpringUtils.getBean(IZaSysErrorService.class);
        // 初始化MQ连接
        String ip = platform.getConfigStr("ip");
        Integer port = platform.getConfigInt("port");
        String vhost = RabbitMqUtil.resolveVhost(platform.getConfigStr("vhost"), SpringUtils.getActiveProfiles());
        String username = platform.getConfigStr("username");
        mqConnectionKey = RabbitMqUtil.connectionKey(ip, port, vhost, username);
        Connection connection = RabbitMqUtil.getConnection(
                ip,
                port,
                vhost,
                username,
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

        ShutdownListener shutdownListener = cause -> {
            if (cause.isInitiatedByApplication()) {
                return;
            }
            log.error("RabbitMQ {} 连接异常断开: {}", ip, cause.getReason());
        };
        queueExchange = platform.getConfigStr("exchange", queueExchange);
        queueName = platform.getConfigStr("queue", platform.getConfigStr("queueName", queueName));
        queueKey = platform.getConfigStr("key", queueKey);
        channel = RabbitMqUtil.produce(connection, queueExchange, queueName, queueKey, args, shutdownListener);
        if (channel == null) {
            log.error("创建RabbitMQ生产队列失败");
            errorService.log(ZaSysError.TYPE_API_ERROR, "创建RabbitMQ生产队列失败", RabbitMqUtil.getException().getMessage(), platform.getConfig());
            throw new IllegalArgumentException("网关初始化失败,创建MQ通道为空!");
        }
        return true;
    }

    @Override
    public boolean stop() {
        RabbitMqUtil.closeQuietly(channel);
        if (mqConnectionKey != null) {
            RabbitMqUtil.closeByKey(mqConnectionKey);
        }
        channel = null;
        mqConnectionKey = null;
        return true;
    }

    @Override
    public boolean isAlive() {
        return channel != null && channel.isOpen();
    }

    @Override
    public void send(String message) {
        log.debug("publish msg to rabbit: {}", message);
        if (channel == null) {
            throw new IllegalStateException("未获取到RabbitMQ的连接,无法处理消息");
        }
        try {
            channel.basicPublish(queueExchange, queueKey, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            SpringUtils.getBean(IZaSysErrorService.class).log(ZaSysError.TYPE_MQ, "RabbitMQ发送消息失败", e.getMessage(), message);
            // 抛出异常由调用方记录推送结果并支持重推
            throw new IllegalStateException("RabbitMQ发送消息失败: " + e.getMessage(), e);
        }
    }
}