package com.zhian.gateway.third.vo;

import lombok.Data;

/**
 * 云台控制参数
 */
@Data
public class PtzVo {
    private Long facilityId;

    /** 指令 */
    private String command;
    /** 速度 */
    private int speed;
}
