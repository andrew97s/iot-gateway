package com.zhian.gateway.web.controller.monitor;

import com.zhian.gateway.common.constant.CacheConstants;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.system.domain.SysCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 缓存监控
 * 
 * @author zhian
 */
@RestController
@RequestMapping("/monitor/cache")
@Slf4j
public class CacheController
{

    @Autowired
    private Cache cache;

    private final static List<SysCache> caches = new ArrayList<SysCache>();
    {
        caches.add(new SysCache(CacheConstants.LOGIN_TOKEN_KEY, "用户信息"));
        caches.add(new SysCache(CacheConstants.SYS_CONFIG_KEY, "配置信息"));
        caches.add(new SysCache(CacheConstants.SYS_DICT_KEY, "数据字典"));
        caches.add(new SysCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        caches.add(new SysCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        caches.add(new SysCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        caches.add(new SysCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping()
    public R getInfo() throws Exception
    {
        return R.success(cache.getInfo());
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getNames")
    public R cache()
    {
        return R.success(caches);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getKeys/{cacheName}")
    public R getCacheKeys(@PathVariable String cacheName)
    {
        Collection<String> keys = cache.keys(cacheName + "*");
        return R.success(keys);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public R getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        Object cacheValue = cache.getCacheObject(cacheKey);
        SysCache sysCache = new SysCache(cacheName, cacheKey, cacheValue.toString());
        return R.success(sysCache);
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public R clearCacheName(@PathVariable String cacheName)
    {
        Collection<String> keys = cache.keys(cacheName + "*");
        cache.deleteObject(keys);
        return R.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public R clearCacheKey(@PathVariable String cacheKey)
    {
        cache.deleteObject(cacheKey);
        return R.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public R clearCacheAll()
    {
        cache.deleteObject(cache.keys("*"));
        return R.success();
    }
}
