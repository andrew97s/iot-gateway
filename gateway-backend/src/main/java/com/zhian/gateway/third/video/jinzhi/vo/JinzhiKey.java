package com.zhian.gateway.third.video.jinzhi.vo;

import lombok.Data;

/**
 * 令牌
 */
@Data
public class JinzhiKey {
    private String mac;
    private String key;
    private String time;
    private String ip;
    private String model;
}
