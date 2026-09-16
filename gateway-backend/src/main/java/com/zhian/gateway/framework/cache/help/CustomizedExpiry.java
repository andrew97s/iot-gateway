package com.zhian.gateway.framework.cache.help;

import com.github.benmanes.caffeine.cache.Expiry;
import com.zhian.gateway.framework.cache.CacheValueWrapper;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * 自定义过期策略 ， 根据缓存value动态生成过期时间
 *
 * @author tongwenjin
 * @since 2023/2/11
 */
@Slf4j
public class CustomizedExpiry implements Expiry<String, CacheValueWrapper> {

    @Override
    public long expireAfterCreate(
            @NonNull String key,
            @NonNull CacheValueWrapper value,
            long currentTime
    ) {
        return value.getCreateExpireTimeMills() - System.currentTimeMillis();
    }

    @Override
    public long expireAfterUpdate(
            @NonNull String key,
            @NonNull CacheValueWrapper value,
            long currentTime,
            @NonNegative long currentDuration
    ) {

        return currentDuration;
    }

    @Override
    public long expireAfterRead(
            @NonNull String key,
            @NonNull CacheValueWrapper value,
            long currentTime,
            @NonNegative long currentDuration
    ) {
        return currentDuration;
    }
}
