package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.util.Map;

@Data
public class JbResponse {
    private Integer code;
    private String message;
    private boolean success;
    private Map data;
}
