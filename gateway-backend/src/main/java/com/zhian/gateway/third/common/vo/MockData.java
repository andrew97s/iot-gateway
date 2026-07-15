package com.zhian.gateway.third.common.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模拟数据
 */
@Data
public class MockData {
    private String auth;
    private String facilityType;
    private String alarmEvent;
    private String analogType;
    private BigDecimal analogValue;

    public boolean isAuth(){
        return auth != null && auth.equalsIgnoreCase("yepanpan");
    }
}
