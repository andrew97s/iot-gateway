package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 获取token请求
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchTokenReq extends EzvizReq {

    private String appKey;

    private String appSecret;

    public FetchTokenReq(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }
}
