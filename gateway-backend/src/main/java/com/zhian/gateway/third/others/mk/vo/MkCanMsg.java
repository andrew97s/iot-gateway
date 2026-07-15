package com.zhian.gateway.third.others.mk.vo;

import lombok.Data;

import java.util.Date;

/**
 * 铭控气瓶消息
 *
 * @author tongwenjin
 * @since 2024-12-4
 */

@Data
public class MkCanMsg {

    /**
     * 消息来源IP
     */
    private String sourceIp;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 设备编码
     */
    private String deviceCode;

    /**
     * 通道号
     */
    private String channel;

    /**
     * 消息记录时间
     */
    private Date time;

    /**
     * 消息业务值
     */
    private String value;

    /**
     * 设备编码 = 设备编码 + 通道号
     *
     * @return 设备编码
     */
    public String getDeviceCode() {
        return deviceCode + "-" + channel;
    }
}
