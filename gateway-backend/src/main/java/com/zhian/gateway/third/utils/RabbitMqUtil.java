package com.zhian.gateway.third.utils;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ连接工具
 */
@Slf4j
public class RabbitMqUtil {

    public static final int DEFAULT_PORT = 5672;
    public static final int DEFAULT_PREFETCH = 50;

    /** 连接池，key = ip|port|vhost|username */
    private static final Map<String, Connection> CONN_MAP = new ConcurrentHashMap<>();
    private static volatile Exception exception;

    public static Exception getException() {
        return exception;
    }

    /**
     * 测试环境强制使用 test vhost，避免和 dev/prod 抢同一队列。
     */
    public static String resolveVhost(String configuredVhost, String[] activeProfiles) {
        if (activeProfiles != null) {
            for (String profile : activeProfiles) {
                if ("test".equals(profile)) {
                    return "test";
                }
            }
        }
        return (configuredVhost == null || configuredVhost.isEmpty()) ? "/" : configuredVhost;
    }

    public static String connectionKey(String ip, Integer port, String vhost, String username) {
        int actualPort = port == null ? DEFAULT_PORT : port;
        String actualVhost = (vhost == null || vhost.isEmpty()) ? "/" : vhost;
        String actualUser = username == null ? "" : username;
        return ip + "|" + actualPort + "|" + actualVhost + "|" + actualUser;
    }

    public static Connection getConnection(String ip, Integer port, String vhost, String username, String password) {
        String key = connectionKey(ip, port, vhost, username);
        Connection cached = CONN_MAP.get(key);
        if (cached != null && cached.isOpen()) {
            return cached;
        }
        if (cached != null) {
            CONN_MAP.remove(key, cached);
            closeQuietly(cached);
        }

        int actualPort = port == null ? DEFAULT_PORT : port;
        String actualVhost = (vhost == null || vhost.isEmpty()) ? "/" : vhost;
        log.info("创建rabbitmq连接: {}:{}@{}", ip, actualPort, actualVhost);
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(ip);
            factory.setPort(actualPort);
            factory.setVirtualHost(actualVhost);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setTopologyRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);
            factory.setRequestedHeartbeat(30);
            factory.setConnectionTimeout(10_000);
            factory.setHandshakeTimeout(10_000);

            Connection connection = factory.newConnection();
            if (connection instanceof Recoverable) {
                ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                    @Override
                    public void handleRecovery(Recoverable recoverable) {
                        log.info("RabbitMQ 连接已恢复: {}", key);
                    }

                    @Override
                    public void handleRecoveryStarted(Recoverable recoverable) {
                        log.warn("RabbitMQ 开始恢复连接: {}", key);
                    }
                });
            }
            Connection previous = CONN_MAP.put(key, connection);
            if (previous != null && previous != connection) {
                closeQuietly(previous);
            }
            return connection;
        } catch (Exception e) {
            exception = e;
            log.error("创建MQ连接失败：{}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建独立 channel 并订阅队列。绑定失败时关闭该 channel 并返回 null。
     */
    public static Channel consume(Connection connection, String exchange, String queue, String key,
                                  DeliverCallback deliverCallback, CancelCallback cancelCallback,
                                  ShutdownListener shutdownListener) {
        return consume(connection, exchange, queue, key, deliverCallback, cancelCallback, shutdownListener, DEFAULT_PREFETCH);
    }

    public static Channel consume(Connection connection, String exchange, String queue, String key,
                                  DeliverCallback deliverCallback, CancelCallback cancelCallback,
                                  ShutdownListener shutdownListener, int prefetch) {
        log.info("创建rabbitmq消费者 @{}, exchange : {}, queue : {}, key：{}", connection.getAddress(), exchange, queue, key);
        Channel channel = null;
        try {
            channel = connection.createChannel();
            if (shutdownListener != null) {
                channel.addShutdownListener(shutdownListener);
            }
            if (!consume(channel, exchange, queue, key, deliverCallback, cancelCallback, prefetch)) {
                closeQuietly(channel);
                return null;
            }
            return channel;
        } catch (Exception e) {
            exception = e;
            log.error("消费MQ - {}连接失败：{}", queue, e.getMessage(), e);
            closeQuietly(channel);
            return null;
        }
    }

    public static boolean consume(Channel channel, String exchange, String queue, String key,
                                  DeliverCallback deliverCallback, CancelCallback cancelCallback) {
        return consume(channel, exchange, queue, key, deliverCallback, cancelCallback, DEFAULT_PREFETCH);
    }

    /**
     * 在已有 channel 上声明并订阅。成功返回 true。
     */
    public static boolean consume(Channel channel, String exchange, String queue, String key,
                                  DeliverCallback deliverCallback, CancelCallback cancelCallback, int prefetch) {
        log.info("rabbitmq订阅消费者 @{}, exchange: {}, queue : {}, key：{}", channel.getConnection().getAddress(), exchange, queue, key);
        try {
            channel.basicQos(prefetch);
            channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, true, false, null);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, exchange, key == null ? "" : key);
            channel.basicConsume(queue, false, deliverCallback, cancelCallback);
            return true;
        } catch (Exception e) {
            exception = e;
            log.error("消费MQ - {}连接失败：{}", queue, e.getMessage(), e);
            return false;
        }
    }

    public static Channel produce(Connection connection, String exchange, String queue, String key,
                                  Map<String, Object> args, ShutdownListener shutdownListener) {
        log.info("创建rabbitmq生产者 @{}: {} : {} ：{}", connection.getAddress(), exchange, queue, key);
        try {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "fanout", true, false, args);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, exchange, key == null ? "" : key);
            if (shutdownListener != null) {
                channel.addShutdownListener(shutdownListener);
            }
            return channel;
        } catch (Exception e) {
            exception = e;
            log.error("生产MQ - {}连接失败：{}", queue, e.getMessage(), e);
            return null;
        }
    }

    public static void ack(Channel channel, long deliveryTag) {
        if (channel == null || !channel.isOpen()) {
            log.warn("ack skipped, channel closed, tag={}", deliveryTag);
            return;
        }
        try {
            channel.basicAck(deliveryTag, false);
        } catch (AlreadyClosedException | IOException e) {
            log.warn("ack failed tag={}: {}", deliveryTag, e.getMessage());
        }
    }

    public static void nack(Channel channel, long deliveryTag, boolean requeue) {
        if (channel == null || !channel.isOpen()) {
            log.warn("nack skipped, channel closed, tag={}, requeue={}", deliveryTag, requeue);
            return;
        }
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (AlreadyClosedException | IOException e) {
            log.warn("nack failed tag={}: {}", deliveryTag, e.getMessage());
        }
    }

    public static boolean close(String ip, Integer port, String vhost, String username) {
        return closeByKey(connectionKey(ip, port, vhost, username));
    }

    /**
     * 按 IP 关闭该地址下全部连接（兼容旧调用）。
     */
    public static boolean close(String ip) {
        boolean closed = false;
        for (Map.Entry<String, Connection> entry : CONN_MAP.entrySet()) {
            if (entry.getKey().equals(ip) || entry.getKey().startsWith(ip + "|")) {
                closeQuietly(entry.getValue());
                CONN_MAP.remove(entry.getKey(), entry.getValue());
                closed = true;
            }
        }
        return closed;
    }

    public static boolean closeByKey(String key) {
        Connection connection = CONN_MAP.remove(key);
        if (connection != null) {
            log.info("关闭RabbitMQ连接 {}", key);
            closeQuietly(connection);
        }
        return true;
    }

    public static void closeQuietly(Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (AlreadyClosedException ignored) {
            // already gone
        } catch (IOException | TimeoutException e) {
            log.warn("关闭RabbitMQ channel失败: {}", e.getMessage());
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (connection.isOpen()) {
                connection.close();
            }
        } catch (AlreadyClosedException ignored) {
            // already gone
        } catch (IOException e) {
            log.warn("关闭RabbitMQ连接失败: {}", e.getMessage());
        }
    }
}
