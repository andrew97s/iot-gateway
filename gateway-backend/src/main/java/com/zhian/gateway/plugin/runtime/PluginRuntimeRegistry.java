package com.zhian.gateway.plugin.runtime;

import com.zhian.gateway.third.PluginHealthResult;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 插件实例运行状态注册中心。
 */
@Component
public class PluginRuntimeRegistry {

    private final Map<String, RuntimeEntry> entries = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> lifecycleLocks = new ConcurrentHashMap<>();

    public ReentrantLock lifecycleLock(String instanceId) {
        return lifecycleLocks.computeIfAbsent(instanceId, key -> new ReentrantLock());
    }

    public void remove(String instanceId) {
        entries.remove(instanceId);
        lifecycleLocks.remove(instanceId);
    }

    public void markStopped(String instanceId, String reason) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            transition(entry, PluginState.STOPPED, reason);
            entry.lastStopTime = new Date();
            entry.consecutiveFailures = 0;
            entry.consecutiveSuccesses = 0;
            entry.restartCount = 0;
            entry.nextRestartTime = null;
        }
    }

    public void markStarting(String instanceId, String reason) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            transition(entry, PluginState.ABNORMAL, reason);
            entry.lastStartTime = new Date();
            entry.consecutiveFailures = 0;
            entry.consecutiveSuccesses = 0;
        }
    }

    public void markRunning(String instanceId, String reason) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            transition(entry, PluginState.RUNNING, reason);
            entry.lastStartTime = new Date();
            entry.consecutiveFailures = 0;
            entry.consecutiveSuccesses = 0;
            entry.restartCount = 0;
            entry.nextRestartTime = null;
        }
    }

    public void markRestarted(String instanceId) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            transition(entry, PluginState.ABNORMAL, "自动重启成功，等待健康检查确认");
            entry.lastStartTime = new Date();
            entry.consecutiveFailures = 0;
            entry.consecutiveSuccesses = 0;
        }
    }

    public void markAbnormal(String instanceId, String reason) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            transition(entry, PluginState.ABNORMAL, reason);
            entry.consecutiveSuccesses = 0;
        }
    }

    public PluginRuntimeSnapshot recordHealth(String instanceId, PluginHealthResult result,
                                              int failureThreshold, int recoveryThreshold) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            entry.lastHealthCheckTime = new Date();
            if (result.isHealthy()) {
                entry.consecutiveFailures = 0;
                entry.consecutiveSuccesses++;
                entry.healthReason = result.getMessage();
                if (entry.state == PluginState.ABNORMAL
                        && entry.consecutiveSuccesses >= Math.max(1, recoveryThreshold)) {
                    transition(entry, PluginState.RUNNING, result.getMessage());
                    entry.restartCount = 0;
                    entry.nextRestartTime = null;
                }
            } else {
                entry.consecutiveSuccesses = 0;
                entry.consecutiveFailures++;
                entry.healthReason = result.getMessage();
                if (entry.consecutiveFailures >= Math.max(1, failureThreshold)) {
                    transition(entry, PluginState.ABNORMAL, result.getMessage());
                }
            }
            return snapshot(entry);
        }
    }

    public boolean prepareAutoRestart(String instanceId, int maxAttempts, long baseDelayMillis) {
        RuntimeEntry entry = entry(instanceId);
        synchronized (entry) {
            long now = System.currentTimeMillis();
            if (entry.state != PluginState.ABNORMAL
                    || entry.restartCount >= Math.max(0, maxAttempts)) {
                return false;
            }
            if (entry.nextRestartTime == null) {
                entry.nextRestartTime = new Date(now + Math.max(1000L, baseDelayMillis));
                return false;
            }
            if (entry.nextRestartTime.getTime() > now) {
                return false;
            }
            entry.restartCount++;
            long multiplier = 1L << Math.min(entry.restartCount, 10);
            entry.nextRestartTime = new Date(now + Math.max(1000L, baseDelayMillis) * multiplier);
            return true;
        }
    }

    public PluginRuntimeSnapshot get(String instanceId) {
        RuntimeEntry entry = entries.get(instanceId);
        if (entry == null) {
            return null;
        }
        synchronized (entry) {
            return snapshot(entry);
        }
    }

    private RuntimeEntry entry(String instanceId) {
        return entries.computeIfAbsent(instanceId, RuntimeEntry::new);
    }

    private void transition(RuntimeEntry entry, PluginState state, String reason) {
        if (entry.state != state) {
            entry.state = state;
            entry.lastStateChangeTime = new Date();
        }
        entry.healthReason = reason;
    }

    private PluginRuntimeSnapshot snapshot(RuntimeEntry entry) {
        PluginRuntimeSnapshot copy = new PluginRuntimeSnapshot();
        copy.setInstanceId(entry.instanceId);
        copy.setState(entry.state);
        copy.setHealthReason(entry.healthReason);
        copy.setLastHealthCheckTime(copy(entry.lastHealthCheckTime));
        copy.setLastStateChangeTime(copy(entry.lastStateChangeTime));
        copy.setLastStartTime(copy(entry.lastStartTime));
        copy.setLastStopTime(copy(entry.lastStopTime));
        copy.setConsecutiveFailures(entry.consecutiveFailures);
        copy.setConsecutiveSuccesses(entry.consecutiveSuccesses);
        copy.setRestartCount(entry.restartCount);
        copy.setNextRestartTime(copy(entry.nextRestartTime));
        return copy;
    }

    private Date copy(Date source) {
        return source == null ? null : new Date(source.getTime());
    }

    private static final class RuntimeEntry {
        private final String instanceId;
        private PluginState state = PluginState.STOPPED;
        private String healthReason = "未启动";
        private Date lastHealthCheckTime;
        private Date lastStateChangeTime = new Date();
        private Date lastStartTime;
        private Date lastStopTime;
        private int consecutiveFailures;
        private int consecutiveSuccesses;
        private int restartCount;
        private Date nextRestartTime;

        private RuntimeEntry(String instanceId) {
            this.instanceId = instanceId;
        }
    }
}
