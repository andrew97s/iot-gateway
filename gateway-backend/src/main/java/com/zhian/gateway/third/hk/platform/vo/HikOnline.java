package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

@Data
public class HikOnline {
    private String deviceIndexCode;
    private String indexCode;
    private String cn;
    private String regionIndexCode;

    /** 在线状态，0离线，1在线*/
    private String online;
}
