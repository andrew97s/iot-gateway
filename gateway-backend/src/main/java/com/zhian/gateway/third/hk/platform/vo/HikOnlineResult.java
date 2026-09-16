package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

/**
 * 海康回返的消息体
 */
@Data
public class HikOnlineResult extends HikResult{
    private HikData data;

    @Data
    public static class HikData{
        private Integer total;
        private Integer pageNo;
        private Integer pageSize;
        private HikOnline[] list;
    }
}
