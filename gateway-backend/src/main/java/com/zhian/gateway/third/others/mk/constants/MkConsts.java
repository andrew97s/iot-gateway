package com.zhian.gateway.third.others.mk.constants;

/**
 * 铭控相关常量
 *
 * @author tongwenjin
 * @since 2024 -12-4
 */
public interface MkConsts {

    /**
     * 设备型号 - S905R
     */
    String DEVICE_MODEL_S905R = "S905R";

    /**
     * 设备类型代码
     */
    String DEVICE_TYPE_CODE = "CP";

    /**
     * 消息类型 - 监测消息
     */
    String MSG_TYPE_MONITOR = "monitor";

    /**
     * 消息类型 - 告警消息
     */
    String MSG_TYPE_ALARM = "alarm";

    /**
     * 告警类型 - 传感器故障
     */
    String ALARM_TYPE_FAULT = "08";
}
