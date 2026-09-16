package com.zhian.gateway.plugin.runtime;

import lombok.Data;

import java.util.Date;

/**
 * 插件实例运行状态快照。
 */
@Data
public class PluginRuntimeSnapshot {
    private String instanceId;
    private PluginState state;
    private String healthReason;
    private Date lastHealthCheckTime;
    private Date lastStateChangeTime;
    private Date lastStartTime;
    private Date lastStopTime;
    private int consecutiveFailures;
    private int consecutiveSuccesses;
    private int restartCount;
    private Date nextRestartTime;
}
