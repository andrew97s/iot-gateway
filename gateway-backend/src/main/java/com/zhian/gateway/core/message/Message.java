package com.zhian.gateway.core.message;

import lombok.Data;

/**
 * 全息网关核心消息
 *
 * @author tongwenjin
 * @since 2026/7/20
 */

@Data
public class Message {

    /**
     * 消息全局唯一ID - 默认为雪花ID
     */
    private String id;

    /**
     * 消息类型: device 设备消息 、 alarm 告警消息 、 telemetry 监测消息 、 control 反控消息
     */
    private String type;

    /**
     * 网关编码 - 默认为系统自动生成不可修改
     */
    private String gatewayCode;

    /**
     * 消息产生时间戳
     */
    private long timestamp;

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 设备信息
     */
    private MessageDevice device;

    /**
     * 消息负载： 根据消息类型的不同, 消息的负载格式也不同
     */
    private MessagePayload payload;
}
