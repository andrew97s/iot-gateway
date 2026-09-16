package com.zhian.gateway.common.core.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存接口
 *
 * @author tongwenjin
 * @since 2023 /2/11
 */
public interface Cache {


    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param <T>   the type parameter
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    <T> void setCacheObject(final String key, final T value);

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param <T>      the type parameter
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit);

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true =设置成功；false=设置失败
     */
    boolean expire(final String key, final long timeout);

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true =设置成功；false=设置失败
     */
    boolean expire(final String key, final long timeout, final TimeUnit unit);

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间 expire
     */
    long getExpire(final String key);

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    Boolean hasKey(String key);

    /**
     * 获得缓存的基本对象。
     *
     * @param <T> the type parameter
     * @param key 缓存键值
     * @return 缓存键值对应的数据 cache object
     */
    <T> T getCacheObject(final String key);

    /**
     * 删除单个对象
     *
     * @param key the key
     * @return the boolean
     */
    boolean deleteObject(final String key);

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return boolean boolean
     */
    boolean deleteObject(final Collection collection);

    /**
     * 缓存List数据
     *
     * @param <T>      the type parameter
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象 cache list
     */
    <T> long setCacheList(final String key, final List<T> dataList);

    /**
     * 获得缓存的list对象
     *
     * @param <T> the type parameter
     * @param key 缓存的键值
     * @return 缓存键值对应的数据 cache list
     */
    <T> List<T> getCacheList(final String key);

    /**
     * 缓存Set
     *
     * @param <T>     the type parameter
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     */
    <T> void setCacheSet(final String key, final Set<T> dataSet);

    /**
     * 获得缓存的set
     *
     * @param <T> the type parameter
     * @param key the key
     * @return cache set
     */
    <T> Set<T> getCacheSet(final String key);

    /**
     * 缓存Map
     *
     * @param <T>     the type parameter
     * @param key     the key
     * @param dataMap the data map
     */
    <T> void setCacheMap(final String key, final Map<String, T> dataMap);

    /**
     * 获得缓存的Map
     *
     * @param <T> the type parameter
     * @param key the key
     * @return cache map
     */
    <T> Map<String, T> getCacheMap(final String key);

    /**
     * 往Hash中存入数据
     *
     * @param <T>   the type parameter
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    <T> void setCacheMapValue(final String key, final String hKey, final T value);

    /**
     * 获取Hash中的数据
     *
     * @param <T>  the type parameter
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象 cache map value
     */
    <T> T getCacheMapValue(final String key, final String hKey);

    /**
     * 获取多个Hash中的数据
     *
     * @param <T>   the type parameter
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合 multi cache map value
     */
    <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys);

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功 boolean
     */
    boolean deleteCacheMapValue(final String key, final String hKey);

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表 collection
     */
    Collection<String> keys(final String pattern);

    /**
     * 获取缓存实列监测数据
     *
     * @return the boolean
     */
    Map<String, Object> getInfo();
}
