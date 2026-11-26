package com.zhian.gateway.framework.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.framework.cache.help.CustomizedExpiry;
import com.zhian.gateway.framework.config.properties.CacheProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于内存实现的缓存
 *
 * @author tongwenjin
 * @since 2023/2/11
 */
@Slf4j
@SuppressWarnings("all")
public class CaffeineCache implements Cache {

    private static volatile com.github.benmanes.caffeine.cache.Cache<String, CacheValueWrapper> caffeine;

    public void init(CacheProperties properties) {
        // 双检锁实现 单例模式
        if (caffeine == null) {
            synchronized (CaffeineCache.class) {
                if (caffeine == null) {

                    if (properties == null || !properties.checkIfValidate()) {
                        throw new IllegalArgumentException("初始化Caffeine失败 , properties 不能为空!");
                    }

                    Caffeine<String, CacheValueWrapper> builder = Caffeine.newBuilder()
                            // 缓存项目过期策略
                            .expireAfter(new CustomizedExpiry())
                            // 缓存项目剔除 事件监听器
                            .evictionListener((key, value, cause) -> {
                                log.info("Caffeine just evict a new item(key: {} , value: {}) for {}", key, value, cause);
                            })
                            // 重写计时器 使用 mill 单位
                            .ticker(new Ticker() {
                                @Override
                                public long read() {
                                    return System.currentTimeMillis();
                                }
                            });

                    if (properties.getMaxCacheSize() != -1) {
                        builder.maximumSize(properties.getMaxCacheSize());
                    }
                    if (properties.getMaxCacheWeight() != -1) {
                        builder.maximumWeight(properties.getMaxCacheWeight());

                        // 在此实现缓存item重量计算逻辑
                        // builder.weigher();
                        throw new UnsupportedOperationException("内存大小计算逻辑暂未实现!");
                    }

                    caffeine = builder.build();
                }
            }
        }
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    @Override
    public <T> void setCacheObject(String key, T value) {
        caffeine.put(key, CacheValueWrapper.newInstance(value, Integer.MAX_VALUE, TimeUnit.DAYS));
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    @Override
    public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit) {
        caffeine.put(key, CacheValueWrapper.newInstance(value, timeout, timeUnit));
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    @Override
    public boolean expire(String key, long timeout) {
       return expire(key , timeout , TimeUnit.MILLISECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);

        if (cacheValue != null) {
            cacheValue.setExpireTime((int) timeout);
            cacheValue.setTimeUnit(unit);

            return true;
        }

        return false;
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    @Override
    public long getExpire(String key) {

        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);

        if (cacheValue != null) {
            return cacheValue.getExpireTime();
        }

        return 0;
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    @Override
    public Boolean hasKey(String key) {
        return caffeine.getIfPresent(key) != null;
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    @Override
    public <T> T getCacheObject(String key) {
        CacheValueWrapper value = caffeine.getIfPresent(key);
        return value != null ? (T) value.getValue() : null;
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    @Override
    public boolean deleteObject(String key) {
        caffeine.invalidate(key);
        return true;
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    @Override
    public boolean deleteObject(Collection collection) {
        caffeine.invalidateAll(collection);
        return true;
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    @Override
    public <T> long setCacheList(String key, List<T> dataList) {
        caffeine.put(key, CacheValueWrapper.newInstance(dataList, Integer.MAX_VALUE, TimeUnit.DAYS));
        return 1;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    @Override
    public <T> List<T> getCacheList(String key) {
        CacheValueWrapper value = caffeine.getIfPresent(key);
        if (value != null) {
            return value.getValue(List.class);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    @Override
    public <T> void setCacheSet(String key, Set<T> dataSet) {
        caffeine.put(key, CacheValueWrapper.newInstance(dataSet, Integer.MAX_VALUE, TimeUnit.DAYS));
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    @Override
    public <T> Set<T> getCacheSet(String key) {
        CacheValueWrapper value = caffeine.getIfPresent(key);
        return value != null ? (Set) value : null;
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    @Override
    public <T> void setCacheMap(String key, Map<String, T> dataMap) {
        caffeine.put(key, CacheValueWrapper.newInstance(dataMap, Integer.MAX_VALUE, TimeUnit.DAYS));
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    @Override
    public <T> Map<String, T> getCacheMap(String key) {
        CacheValueWrapper value = caffeine.getIfPresent(key);
        return value != null ? value.getValue(Map.class) : null;
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    @Override
    public <T> void setCacheMapValue(String key, String hKey, T value) {
        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);
        if (cacheValue != null && cacheValue.getValue() instanceof Map) {
            Map map = cacheValue.getValue(Map.class);
            map.put(hKey, value);
        } else {
            HashMap<Object, Object> map = new HashMap<>();
            map.put(hKey, value);
            caffeine.put(key, CacheValueWrapper.newInstance(value, Integer.MAX_VALUE, TimeUnit.DAYS));
        }
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    @Override
    public <T> T getCacheMapValue(String key, String hKey) {
        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);

        return cacheValue != null ? (T) cacheValue.getValue(Map.class).get(hKey) : null;
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    @Override
    public <T> List<T> getMultiCacheMapValue(String key, Collection<Object> hKeys) {

        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);

        if (cacheValue != null) {
            List result = new ArrayList<>();
            Map map = cacheValue.getValue(Map.class);
            for (Object hKey : hKeys) {
                result.add(map.get(hKey));
            }

            return result;
        } else {
            return null;
        }
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    @Override
    public boolean deleteCacheMapValue(String key, String hKey) {

        CacheValueWrapper cacheValue = caffeine.getIfPresent(key);

        if (cacheValue != null) {
            Map map = cacheValue.getValue(Map.class);
            map.remove(hKey);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    @Override
    public Collection<String> keys(String pattern) {

        List<String> results = new ArrayList<>();

        if (pattern.endsWith("*")) {
            pattern = pattern.substring(0 , pattern.lastIndexOf("*"));
        }

        String finalPattern = pattern;
        caffeine.asMap().keySet().forEach(key -> {
            if (key.startsWith(finalPattern)) {
                results.add(key);
            }
        });

        return results;
    }

    @Override
    public Map<String, Object> getInfo() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("stats" , caffeine.stats().toString());
        log.info("caffeine stats: {}" , caffeine.stats().toString());
        return map;
    }

}
