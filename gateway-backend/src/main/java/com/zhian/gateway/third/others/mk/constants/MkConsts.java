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
     * 设备类型代码 - 钢瓶压力
     */
    String DEVICE_TYPE_CODE = "CP";

    /**
     * 设备类型 - 无线仪表（压力）
     */
    String DEVICE_TYPE_PRESSURE = "WMETER";

    /**
     * 设备类型 - 水箱（池）液位
     */
    String DEVICE_TYPE_LEVEL = "LLOWT";

    /**
     * 设备类型 - 消防水泵
     */
    String DEVICE_TYPE_PUMP = "FIREP";

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

    /**
     * 青鸟定制协议设备类别
     */
    String KIND_PRESSURE = "pressure";
    String KIND_LEVEL = "level";
    String KIND_PUMP = "pump";

    /**
     * 青鸟定制 - 心跳
     */
    int JB_TYPE_HEARTBEAT = 0;
    /**
     * 青鸟定制 - 定时上报
     */
    int JB_TYPE_PERIOD = 1;
    /**
     * 青鸟定制 - 变化上报
     */
    int JB_TYPE_CHANGE = 2;
    /**
     * 青鸟定制 - 电池电压低
     */
    int JB_TYPE_LOW_BATTERY = 3;
    /**
     * 青鸟定制 - 设备故障
     */
    int JB_TYPE_FAULT = 4;
    /**
     * 青鸟定制 - 定时上报（液位厘米）
     */
    int JB_TYPE_PERIOD_CM = 5;
    /**
     * 青鸟定制 - 变化上报（液位厘米）
     */
    int JB_TYPE_CHANGE_CM = 6;
    /**
     * 青鸟定制 - 应答 / 设备回复
     */
    int JB_TYPE_ACK = 0xFFFF;
    /**
     * 青鸟定制 - 下发上限
     */
    int JB_CMD_SET_HIGH = 1;
    /**
     * 青鸟定制 - 下发下限
     */
    int JB_CMD_SET_LOW = 2;
    /**
     * 青鸟定制 - 全部设置
     */
    int JB_CMD_SET_ALL = 3;

    /**
     * 高限报警
     */
    String ALARM_HIGH = "45";
    /**
     * 低限报警
     */
    String ALARM_LOW = "46";
    /**
     * 电池欠压故障
     */
    String ALARM_LOW_BATTERY = "62";
    /**
     * 设备故障
     */
    String ALARM_DEVICE_FAULT = "169";
    /**
     * 水泵启动（开启）
     */
    String ALARM_PUMP_ON = "11";
    /**
     * 水泵停止
     */
    String ALARM_PUMP_OFF = "13";
    /**
     * 放水告警
     */
    String ALARM_WATER_DISCHARGE = "1113";
    /**
     * 井盖开启告警
     */
    String ALARM_WELL_OPEN = "1114";
    /**
     * 开盖
     */
    String ALARM_COVER_OPEN = "140";
    /**
     * 倾倒 / 倾斜
     */
    String ALARM_TILT = "60";
    /**
     * 振动 / 撞击
     */
    String ALARM_SHOCK = "138";
    /**
     * 传感器故障
     */
    String ALARM_SENSOR_FAULT = "177";
}
