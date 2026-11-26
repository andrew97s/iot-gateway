package com.zhian.gateway.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性
 *
 * @author tongwenjin
 * @since 2023 /2/11
 */
@ConfigurationProperties("zhian.cache")
@Data
public class CacheProperties {

    private Mode mode;

    /**
     * 最大缓存key数量 单位 个
     */
    private int maxCacheSize = -1;

    /**
     * 最大缓存容量 单位 为Mb
     */
    private int maxCacheWeight = -1;

    public boolean checkIfValidate() {
        maxCacheWeight = maxCacheWeight > 0 ? maxCacheWeight : -1;
        maxCacheSize = maxCacheSize > 0 ? maxCacheSize : -1;

        if (maxCacheSize <= 0 && maxCacheWeight <= 0) {
            throw new IllegalArgumentException("最大缓存数量与最大缓存大小不能同时为空!");
        } else if (maxCacheSize >= 0 && maxCacheWeight >= 0) {
            throw new IllegalArgumentException("最大缓存数量与最大缓存大小只能同时设置一项!");
        }

        return true;
    }

    /**
     * The enum Mode.
     *
     * @author zhian
     * @since 2022 -09-09
     */
    public static enum Mode {
        /**
         * Redis mode.
         */
        REDIS,

        /**
         * Memory mode.
         */
        MEMORY
    }
}
