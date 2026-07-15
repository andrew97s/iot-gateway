package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;

/**
 * 萤石云基础响应
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
public class EzvizResp {

    private String msg;

    private String code;

    private Object data;

    public boolean isSuccess() {
        return getCode().equals("200") && getData() != null;
    }
}
