package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询设备流响应
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchDeviceStreamResp extends EzvizResp{

    private StreamData data;


    @Data
    public static class StreamData {

        private String id;

        private String url;

        private String expireTime;
    }
}
