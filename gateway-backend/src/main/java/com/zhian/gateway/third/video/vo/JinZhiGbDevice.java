package com.zhian.gateway.third.video.vo;

import lombok.Data;

@Data
public class JinZhiGbDevice {
    private String deviceID;
    private String channelID;
    private String channelName;
    private String isOnline;
    private String username;
    private String password;
    private String ip;
    private String type;
    private String streamUrl;
    private Integer channelCount;
}
