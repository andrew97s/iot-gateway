package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

/**
 * 青鸟云盒上报的消息
 */
@Data
public class BoxMsg {
    public static final String TYPE_HEARTBEAT = "heartbeat";
    public static final String TYPE_REGISTER = "register";
    public static final String TYPE_REGISTER_CAMERA = "register-camera";
    public static final String TYPE_ALARM = "alarm";
    private String type;
    private String msg;
}
