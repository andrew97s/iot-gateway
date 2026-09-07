package com.zhian.gateway.third.hk.ezviz.vo;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

/**
 * api请求
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
public class WebhookReq {

    private Header header;

    private String body;

    public abstract static class WebhookBody {

    }

    public <T extends WebhookBody > T getReqBody(Class<T> bodyClass) {
        return JSON.parseObject(body , bodyClass);
    }

    @Data
    public static class Header {
        private Long channelNo;

        private String deviceId;

        private String messageId;

        private Long messageTime;

        private String type;
    }
}
