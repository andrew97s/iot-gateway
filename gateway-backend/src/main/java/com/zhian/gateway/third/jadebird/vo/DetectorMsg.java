package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

/**
 * 青瞳上报的消息
 */
@Data
public class DetectorMsg {
    public static final String TYPE_HEARTBEAT = "heartbeat";
    public static final String TYPE_REGISTER_DETECTOR_LIST = "registerDetectorList";
    public static final String TYPE_REGISTER_DEVICE = "registerDevice";
    public static final String TYPE_ALARM = "alarm";
    public static final String TYPE_CANCEL_ALARM = "cancelAlarm";
    private String type;
    private String msg;
    private String ip;
}
