package com.zhian.gateway.third.dahua.vo;

import com.alibaba.fastjson2.JSONObject;
import com.dahuatech.icc.oauth.http.IccResponse;
import lombok.Data;

/**
 * ICC平台通用返回结果
 */
@Data
public class IccCommonResponse extends IccResponse {

    private JSONObject data;

}
