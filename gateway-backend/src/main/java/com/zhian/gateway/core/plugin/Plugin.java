package com.zhian.gateway.core.plugin;

import com.zhian.gateway.sys.domain.ZaSysPlatform;

/**
 * 全息网关核心接入插件
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
public interface Plugin {

    void start(ZaSysPlatform platform);

    void stop(String reason);

    boolean isAlive(String reason);
}
