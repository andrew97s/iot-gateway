package com.zhian.gateway.plugin;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 接入插件实例视图（管理端列表/详情）
 */
@Data
public class PluginInstanceView {
    private Long id;
    /** 实例编码（当前与插件类型 1:1，即 za_sys_platform.code） */
    private String instanceId;
    private String pluginId;
    private String name;
    private String vendor;
    private String protocol;
    private String version;
    private List<String> capabilities;
    private String ip;
    private Integer port;
    private String apis;
    private String remark;
    /** installed / configured / running / stopped / abnormal */
    private String state;
    private String status;
    private String running;
    private Map<String, Object> config;
    private List<Map<String, Object>> configSchema;
    private boolean alive;
    private String connectionInfo;
    private Long deviceCount;
    private Long todayMsgCount;
    private Long todayFailCount;
    private Long msgCount;
    private Long errCount;
    private Integer restartCount;
    private Date lastStartTime;
    private Date lastStopTime;
}
