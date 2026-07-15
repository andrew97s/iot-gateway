package com.zhian.gateway.third.hk.ezviz.consts;

/**
 * 萤石云相关常量
 *
 * @author tongwenjin
 * @since 2024 -11-28
 */
public interface EzvizConstants {

    /**
     * 消息类型 - 告警
     */
    String MSG_TYPE_ALARM = "ys.alarm";

    /**
     * 消息类型 - 离在线
     */
    String MSG_TYPE_ONLINE = "ys.onoffline";

    /**
     * 消息类型 - 设备状态
     */
    String MSG_TYPE_STATUS = "ys.devicestatus";

    /**
     * 消息类型 - ISAPI上行(心跳)
     */
    String MSG_TYPE_API = "ys.open.isapi";

    /**
     * 设备上线
     */
    String DEVICE_ONLINE = "ONLINE";

    /**
     * 设备离线
     */
    String DEVICE_OFFLINE = "OFFLINE";

    /**
     * 告警类型 - 烟感告警
     */
    String ALARM_TYPE_FIRE = "fire";

    /**
     * 告警类型 - 遮挡告警
     */
    String ALARM_TYPE_SHELTER = "shelteralarm";

    /**
     * 告警类型 - 视频丢失
     */
    String ALARM_TYPE_VIDEO_LOSS = "videoloss";

    /**
     * 告警类型 - 电池电量低告警
     */
    String ALARM_TYPE_UNDER_VOLTAGE = "UnderVoltage";

    /**
     * 告警类型 - 遮挡告警
     */
    String ALARM_TYPE_DEV_FAILURE = "DeviceFailure";
    /**
     * 告警类型 - 火警告警
     */
    String ALARM_TYPE_FIRE_TRIG = "fireTrig";

    /**
     * 告警类型 - 火警告警恢复
     */
    String ALARM_TYPE_FIREREST = "fireRest";
    /**
     * 告警类型 - 设备防拆告警
     */
    String ALARM_TYPE_DEV_TAMPERING = "devTampering";

    /**
     * 告警类型 - 设备防拆告警恢复
     */
    String ALARM_TYPE_DEV_TAMPER_RECOVER = "devTamperRecov";

    /**
     * 告警类型 - SIM卡异常
     */
    String ALARM_TYPE_DEV_SIM_EXCEP = "SIMExcep";
    /**
     * 告警类型 - SIM卡恢复正常
     */
    String ALARM_TYPE_DEV_SIM_RECOV = "SIMRecov";
    /**
     * 告警类型 - 撤防
     */
    String ALARM_TYPE_DEV_DISARM = "disarm";
    /**
     * 告警类型 - 消警
     */
    String ALARM_TYPE_CLEAR = "clear";
    /**
     * 告警类型 - 硬盘满
     */
    String ALARM_TYPE_HDD_FULL = "HDDfull";
    /**
     * 告警类型 - 硬盘空闲
     */
    String ALARM_TYPE_HDD_FREE = "HDDfree";
    /**
     * 告警类型 - 硬盘出错
     */
    String ALARM_TYPE_HDD_EXCEPTION = "HDDexception";
    /**
     * 告警类型 - 硬盘恢复正常
     */
    String ALARM_TYPE_HDD_RECOV = "HDDRecov";
    /**
     * 告警类型 - 烟雾告警
     */
    String ALARM_TYPE_SMOKE_ALARM = "smokeAlarm";
}
