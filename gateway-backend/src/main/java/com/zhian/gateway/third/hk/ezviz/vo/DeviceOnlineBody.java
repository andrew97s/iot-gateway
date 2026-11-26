package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备上下线请求体
 *
 * @author tongwenjin
 * @since 2024 -11-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceOnlineBody extends WebhookReq.WebhookBody {

    /**
     * 设备序列号
     */
    private String subSerial;
    /**
     * 设备上线（离线）时间，格式： yyyy-MM-dd HH:mm:ss
     */
    private String occurTime;
    /**
     * 设备上一次注册时间，格式： yyyy-MM-dd HH:mm:ss
     */
    private String regTime;
    /**
     * 设备外网IP
     */
    private String natIp;
    /**
     * 设备类型
     */
    private String devType;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 消息类型：OFFLINE-设备离线消息，ONLINE-设备上线消息
     */
    private String msgType;
}
