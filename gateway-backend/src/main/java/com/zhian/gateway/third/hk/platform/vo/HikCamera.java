package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

@Data
public class HikCamera {
    private String cameraIndexCode;
    private String cameraName;
    private Integer cameraType;
    private String channelNo;
    private String regionIndexCode;

    /** 在线状态（0-未知，1-在线，2-离线），扩展字段，暂不使用*/
    private String status;
}
