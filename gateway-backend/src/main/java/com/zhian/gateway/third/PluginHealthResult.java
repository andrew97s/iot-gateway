package com.zhian.gateway.third;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 插件健康检查结果。
 */
@Data
@AllArgsConstructor
public class PluginHealthResult {
    private boolean healthy;
    private String message;

    public static PluginHealthResult healthy(String message) {
        return new PluginHealthResult(true, message == null ? "连接正常" : message);
    }

    public static PluginHealthResult unhealthy(String message) {
        return new PluginHealthResult(false, message == null ? "连接异常" : message);
    }
}
