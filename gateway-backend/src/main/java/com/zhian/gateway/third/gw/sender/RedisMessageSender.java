package com.zhian.gateway.third.gw.sender;

import com.zhian.gateway.sys.domain.ZaSysPlatform;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;

import static com.zhian.gateway.third.gw.consts.GatewayConstants.CACHE_QUEUE_NAME;

/**
 * redis实现缓存队列的消息发送器
 *
 * @author tongwenjin
 * @since 2024-12-9
 */
@Slf4j
public class RedisMessageSender implements MessageSender {

    private StatefulRedisConnection<String, String> connection;
    private RedisClient redisClient;

    @Override
    public boolean start(ZaSysPlatform platform) {
        // 获取默认配置
        String host = platform.getConfigStr("ip", "127.0.0.1");
        String port = platform.getConfigStr("port", "6379");
        String password = platform.getConfigStr("password", "Pa33w0rd");
        String db = platform.getConfigStr("db", "6");

        // 创建 Redis 客户端
        redisClient = RedisClient.create();
        // 创建连接
        connection = redisClient.connect(
                RedisURI.builder()
                        .withHost(host)
                        .withPort(Integer.parseInt(port))
                        .withDatabase(Integer.parseInt(db))
                        .withPassword(password.toCharArray())
                        .build()
        );

        return true;
    }

    @Override
    public boolean stop() {
        if (connection != null && connection.isOpen()) {
            connection.close();
            connection = null;
            redisClient.shutdown();
            redisClient = null;
        }
        return true;
    }

    @Override
    public boolean isAlive() {
        return connection != null && connection.isOpen();
    }

    @Override
    public void send(String message) {
        Long queueLength = connection.sync().lpush(CACHE_QUEUE_NAME, message);
        log.debug("message successfully send via redis queue(length:{})!", queueLength);
    }
}
