package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 获取token响应
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchTokenResp extends EzvizResp {

    private Data data;


    @lombok.Data
    public static class Data {
        private String accessToken;

        private Long expireTime;
    }
}
