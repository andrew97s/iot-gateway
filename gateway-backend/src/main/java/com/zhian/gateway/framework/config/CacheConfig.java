package com.zhian.gateway.framework.config;

import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.framework.cache.CaffeineCache;
import com.zhian.gateway.framework.cache.RedisCache;
import com.zhian.gateway.framework.config.properties.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存配置类
 *
 * @author tongwenjin
 * @since 2023/2/11
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@SuppressWarnings("all")
@Import(RedisConfig.class)
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "zhian.cache" , value = "mode" , havingValue = "REDIS")
    public Cache redisCache(RedisTemplate redisTemplate) {
        return new RedisCache(redisTemplate);
    }

    @ConditionalOnProperty(prefix = "zhian.cache" , value = "mode" , havingValue = "MEMORY")
    @Bean Cache caffeineCache(CacheProperties properties) {
        CaffeineCache cache = new CaffeineCache();
        cache.init(properties);

        return cache;
    }
}
