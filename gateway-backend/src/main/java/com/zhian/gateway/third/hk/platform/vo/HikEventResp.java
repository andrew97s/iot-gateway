package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;

/**
 * 海康事件响应
 *
 * @author tongwenjin
 * @since 2024/12/11
 */
@Data
public class HikEventResp {

    private String method;
    private String params;
}
