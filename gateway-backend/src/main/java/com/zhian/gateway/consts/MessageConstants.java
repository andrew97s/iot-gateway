package com.zhian.gateway.consts;

/**
 * 消息相关常量
 *
 * @author tongwenjin
 * @since 2026/7/30
 */
public interface MessageConstants {
    /**
     * 告警.
     */
    String MSG_TYPE_ALARM = "alarm";
    /**
     * 设备新增.
     */
    String MSG_TYPE_DEVICE_ADD = "device_add";
    /**
     * 设备更新.
     */
    String MSG_TYPE_DEVICE_UPD = "device_upd";
    /**
     * 设备删除.
     */
    String MSG_TYPE_DEVICE_DEL = "device_del";
    /**
     * 监测.
     */
    String MSG_TYPE_TELEMETRY = "telemetry";
    /**
     * 反控.
     */
    String MSG_TYPE_CONTROL = "control";
}
