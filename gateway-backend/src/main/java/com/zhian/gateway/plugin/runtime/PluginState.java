package com.zhian.gateway.plugin.runtime;

/**
 * 插件对外逻辑状态。状态只存在于运行内存，不持久化到平台表。
 */
public enum PluginState {
    INSTALLED("installed"),
    STOPPED("stopped"),
    RUNNING("running"),
    ABNORMAL("abnormal");

    private final String code;

    PluginState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
