package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

/**
 * 海康回返的消息体
 */
@Data
public class HikResult {
    private String code;
    private String msg;

    public boolean isSuccess(){
        return code != null && "0".equalsIgnoreCase(code);
    }
}
