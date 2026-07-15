package com.zhian.gateway.third.gw.consts;

/**
 * 网关相关常量
 *
 * @author tongwenjin
 * @since 2024 /8/13
 */
public interface GatewayConstants {

    /**
     * 网关对接方式 - MQ.
     */
    String PUSH_TYPE_MQ = "mq";

    /**
     * 网关对接方式 - URL.
     */
    String PUSH_TYPE_URL = "url";

    /**
     * 网关对接方式 - REDIS.
     */
    String PUSH_TYPE_REDIS = "redis";

    /**
     * 缓存队列名称
     */
    String CACHE_QUEUE_NAME = "GATEWAY_QUEUE";
}
