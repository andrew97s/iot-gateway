package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

/**
 * 云盒V2信息VO
 *
 * @author tongwenjin
 * @since 2024-9-25
 */

@Data
public class BoxV2Info {
    // 合作商编码
    private String code;
    // 盒子设备唯一标识
    private String serialNo;
    // 当前时间
    private String currTime;
    // 盒子ip
    private String ip;
    // 盒子名称
    private String name;
}
