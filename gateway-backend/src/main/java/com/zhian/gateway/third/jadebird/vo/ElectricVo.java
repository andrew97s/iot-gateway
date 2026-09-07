package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ElectricVo {
    /** 相位 **/
    private String phase;
    /** 单位 **/
    private String analogType;
    /** 监测值 **/
    private BigDecimal analogValue;
}
