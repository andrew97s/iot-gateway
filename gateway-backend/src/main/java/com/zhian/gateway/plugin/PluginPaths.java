package com.zhian.gateway.plugin;

import com.zhian.gateway.common.config.ZhianConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 插件包本地存储路径
 */
public final class PluginPaths {
    private PluginPaths() {}

    public static Path packagesRoot() {
        String profile = ZhianConfig.getProfile();
        if (profile == null || profile.isEmpty()) {
            profile = System.getProperty("user.dir") + "/upload";
        }
        return Paths.get(profile, "plugins");
    }

    public static Path packageDir(String pluginId) {
        return packagesRoot().resolve(pluginId);
    }

    public static Path instanceLogDir(String instanceId) {
        String profile = ZhianConfig.getProfile();
        if (profile == null || profile.isEmpty()) {
            profile = System.getProperty("user.dir") + "/upload";
        }
        return Paths.get(profile, "logs", "plugins", instanceId);
    }
}
