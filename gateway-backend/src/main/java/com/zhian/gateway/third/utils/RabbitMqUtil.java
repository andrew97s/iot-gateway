package com.zhian.gateway.third.utils;

import com.rabbitmq.client.*;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ连接工具
 */
@Slf4j
public class RabbitMqUtil  {

    /** 连接池**/
    private static Map<String, Connection> chnMap = new ConcurrentHashMap<>();
    private static Exception exception;
    public static Exception getException(){
        return exception;
    }

    /**
     * 获取Rbbit连接
     * @param ip
     * @param port
     * @param vhost
     * @param username
     * @param password
     * @return
     */
    public static Connection getConnection(String ip, Integer port, String vhost, String username, String password){

        Connection connection  = chnMap.get(ip);
        if(connection != null){
            return connection;
        }
        log.info("创建rabbitmq连接: {}:{}@{}", ip,  port, vhost);
        try {
            //创建一个连接工厂
            ConnectionFactory connectionFactory = new ConnectionFactory();
            //设置要连接RabbitMQ所在服务器的ip地址，这里要进行填写自己的
            connectionFactory.setHost(ip);
            connectionFactory.setPort(port);
            connectionFactory.setVirtualHost(vhost);
            //设置账号
            connectionFactory.setUsername(username);
            //设置密码
            connectionFactory.setPassword(password);
            connection = connectionFactory.newConnection();
            chnMap.put(ip, connection);
        }catch (Exception e){
            e.printStackTrace();
            RabbitMqUtil.exception = e;
            log.error("创建MQ连接失败：{}", e);
        }
        return  connection;
    }

    /**
     * 绑定消费
     * @param connection
     * @param exchange
     * @param queue
     * @param key
     * @param deliverCallback
     * @param cancelCallback
     * @param shutdownListener
     * @return
     */
    public static Channel consume(Connection connection, String exchange, String queue, String key, DeliverCallback deliverCallback, CancelCallback cancelCallback, ShutdownListener shutdownListener) {
        log.info("创建rabbitmq消费者 @{}, exchange : {}, queue : {}, key：{}", connection.getAddress(), exchange,  queue, key);
        try {
            Channel channel  = connection.createChannel();

            channel.addShutdownListener(shutdownListener);

            consume(channel, exchange, queue, key, deliverCallback, cancelCallback);
            return channel;
        }catch (Exception e){
            e.printStackTrace();
            RabbitMqUtil.exception = e;
            log.error("消费MQ - {}连接失败：{}", queue, e);
            return null;
        }
    }


    /**
     * 绑定消费
     * @param channel
     * @param exchange
     * @param queue
     * @param key
     * @param deliverCallback
     * @param cancelCallback
     * @return
     */
    public static void consume(Channel channel, String exchange, String queue, String key, DeliverCallback deliverCallback, CancelCallback cancelCallback) {
        log.info("rabbitmq订阅消费者 @{}, exchange: {}, queue : {}, key：{}", channel.getConnection().getAddress(), exchange,  queue, key);
        try {

            // 主动申明交换机和对应队列
            channel.exchangeDeclare(exchange, BuiltinExchangeType.FANOUT, true, false, null);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, exchange, key);

            /**
             * 消费者消费消息
             * 1. 消费哪个队列
             * 2. 消费成功后是否自动应答
             * 3. 消费成功的回调接口
             * 4. 消费取消的回调接口
             */
            channel.basicConsume(queue,false, deliverCallback,cancelCallback);

        }catch (Exception e){
            e.printStackTrace();
            RabbitMqUtil.exception = e;
            log.error("消费MQ - {}连接失败：{}", queue, e);
        }
    }

    /**
     * 绑定生产
     * @param connection
     * @param exchange
     * @param queue
     * @param key
     * @param args
     * @param shutdownListener
     * @return
     */
    public static Channel produce(Connection connection, String exchange, String queue, String key, Map<String, Object> args, ShutdownListener shutdownListener) {
        log.info("创建rabbitmq生产者 @{}: {} : {} ：{}", connection.getAddress(), exchange,  queue, key);
        try {
            Channel channel  = connection.createChannel();
            channel.exchangeDeclare(exchange, "fanout", true, false, args);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, exchange, key);

            channel.addShutdownListener(shutdownListener);
            return channel;
        }catch (Exception e){
            e.printStackTrace();
            RabbitMqUtil.exception = e;
            log.error("生产MQ - {}连接失败：{}", queue, e);
            return null;
        }
    }

    /**
     * 断开连接
     * @param ip
     * @return
     */
    public static boolean close(String ip) {
        Connection connection = chnMap.get(ip);
        if (connection != null) {
            log.info("关闭RabbitMQ连接 {}", ip);
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            chnMap.remove(ip);
        }
        return true;
    }
}
