package com.zhian.gateway.third.video.vo;

import lombok.Data;

@Data
public class JinZhiDevice {
    private String deviceID;
    private String name;
    private String type;
    private String ip;
    private String username;
    private String password;
    private String streamUrl;
    private String streamSubUrl;
    private String exidgb;
}
