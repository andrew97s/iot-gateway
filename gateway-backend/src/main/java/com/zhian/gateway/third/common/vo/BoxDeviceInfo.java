package com.zhian.gateway.third.common.vo;

import lombok.Data;

@Data
public class BoxDeviceInfo {
    private String deviceId;

    /** 0、设备下线1、设备上线 */
    private Integer messageType;

    private String deviceModel;
    private String ip;

}
