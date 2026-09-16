package com.zhian.gateway.framework.cache;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * Cache 缓存 value 包装类
 *
 * @author tongwenjin
 * @since 2023 /2/11
 */
@Data
public class CacheValueWrapper {

    private int expireTime;

    private TimeUnit timeUnit;

    private ExpiryMode expireMode;

    private Object value;

    private long createTime;

    private long lastUpdateTime;

    private long lastReadTime;

    /**
     * Gets value.
     *
     * @param <T>   the type parameter
     * @param clazz the clazz
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> clazz) {
        lastReadTime = System.currentTimeMillis();
        return (T) value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(Object value) {
        lastUpdateTime = System.currentTimeMillis();
        this.value = value;
    }

    /**
     * Gets create expire time mills.
     *
     * @return the create expire time mills
     */
    public long getCreateExpireTimeMills() {
        return createTime + timeUnit.toMillis(expireTime);
    }

    /**
     * Gets update expire time mills.
     *
     * @return the update expire time mills
     */
    public long getUpdateExpireTimeMills() {
        return lastUpdateTime + timeUnit.toMillis(expireTime);
    }

    /**
     * Gets read expire time mills.
     *
     * @return the read expire time mills
     */
    public long getReadExpireTimeMills() {
        return lastReadTime + timeUnit.toMillis(expireTime);
    }

    private CacheValueWrapper(int expireTime, TimeUnit timeUnit, ExpiryMode expireMode, Object value) {
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
        this.expireMode = expireMode;
        this.value = value;
    }

    /**
     * 初始化一个cache value wrapper实例
     *
     * @param value      the value
     * @param expireTime the expire time
     * @param timeUnit   the time unit
     * @return the cache value wrapper
     */
    public static CacheValueWrapper newInstance(Object value, int expireTime, TimeUnit timeUnit) {

        CacheValueWrapper valueWrapper = new CacheValueWrapper(expireTime, timeUnit, ExpiryMode.AFTER_CREATE, value);

        valueWrapper.setCreateTime(System.currentTimeMillis());
        valueWrapper.setLastUpdateTime(valueWrapper.createTime);

        return valueWrapper;
    }

    /**
     * 过期模式
     *
     * @author tongwenjin
     * @since 2022 -09-09
     */
    public static enum ExpiryMode {
        /**
         * 创建后开始计时
         */
        AFTER_CREATE,

        /**
         * 最后依次读取后开始计时
         */
        AFTER_READ,

        /**
         * 最后依次写入后开始计时
         */
        AFTER_WRITE
    }
}
