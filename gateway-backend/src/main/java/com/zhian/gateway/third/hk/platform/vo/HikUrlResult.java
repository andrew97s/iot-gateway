package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

/**
 * 海康回返的消息体
 */
@Data
public class HikUrlResult extends HikResult{
    private HikUrl data;
    @Data
    public static class HikUrl{
        private String url;
        private String picUrl;
    }
}
