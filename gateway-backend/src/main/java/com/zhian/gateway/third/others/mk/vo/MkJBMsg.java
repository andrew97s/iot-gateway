package com.zhian.gateway.third.others.mk.vo;

import lombok.Data;

/**
 * 铭控青鸟定制协议消息（三水：压力 / 液位 / 水泵）
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Data
public class MkJBMsg {

    /**
     * 消息来源IP
     */
    private String sourceIp;

    /**
     * 设备类别：pressure / level / pump
     */
    private String kind;

    /**
     * 协议数据类型
     */
    private int dataType;

    /**
     * 设备编号
     */
    private String code;

    /**
     * 上限
     */
    private String thresholdHigh;

    /**
     * 下限
     */
    private String thresholdLow;

    /**
     * 当前值（压力/液位为数值，水泵为 0关/1开）
     */
    private String value;

    /**
     * 数值单位：kPa / mm / cm / status
     */
    private String unit;

    /**
     * 数据时间戳（毫秒）
     */
    private long timestamp;

    /**
     * SIM 卡 ICCID
     */
    private String iccid;
}
