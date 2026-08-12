package com.zhian.gateway.core.message;

import lombok.Data;

/**
 * 全息网关核心消息-设备
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
@Data
public class MessageDevice {

    private long deviceId;

    private String code;

    private String net;

    private String name;

    private String typeCode;

    private String online;
}
