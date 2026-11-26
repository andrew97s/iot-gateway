package com.zhian.gateway.api.domain;

import lombok.Data;

/**
 * 视频请求
 */
@Data
public class VideoRequest {
    public static final String TYPE_FLV = "flv";
    public static final String TYPE_M3U8 = "m3u8";
    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_WS = "ws";
    public static final String PROTOCOL_RTSP = "rtsp";

    private Long deviceId;
    private String videoType;
    private String protocol;
}
