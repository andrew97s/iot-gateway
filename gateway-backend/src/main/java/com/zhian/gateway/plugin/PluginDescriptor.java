package com.zhian.gateway.plugin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 接入插件类型描述（对应设计原型 plugin.yaml）
 */
@Data
public class PluginDescriptor {
    /** 插件类型 ID，与 ThirdHandler.getPlatform() 对齐 */
    private String id;
    private String name;
    private String vendor;
    private String protocol;
    private String version;
    /** 能力声明：alarm / monitor / device / control 等 */
    private List<String> capabilities = new ArrayList<>();
    private String description;
    /** 动态配置表单 schema */
    private List<Map<String, Object>> configSchema = new ArrayList<>();
    /** 是否已有可运行的 Handler 实现 */
    private boolean runnable;
    /** 是否来自已安装的插件包 */
    private boolean fromPackage;
    /** 插件包本地目录（可选） */
    private String packagePath;
}
