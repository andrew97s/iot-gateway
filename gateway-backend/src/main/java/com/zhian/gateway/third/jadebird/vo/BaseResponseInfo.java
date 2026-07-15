package com.zhian.gateway.third.jadebird.vo;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * The type Base response info.
 */
@Data
public class BaseResponseInfo {
    /**
     * The Code.
     */
    private Integer code;
    /**
     * The Message.
     */
    private String message;
    /**
     * The Success.
     */
    private boolean success;
    /**
     * The Data.
     */
    private BaseResponseBody data;

    /**
     * Is success boolean.
     *
     * @return the boolean
     */
    public boolean isSuccess() {
        return data != null && StrUtil.isNotBlank(data.getToken());
    }
}
