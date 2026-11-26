package com.zhian.gateway.third.video.jinzhi.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * 响应对象
 */
@Data
public class JinzhiWsResp {
    public static final String VERSION = "0.2.2";
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAIL = "fail";

    private String version;
    private String action;
    private String result;
    private JSONObject param;

    public static JinzhiWsResp success(String action){
        JinzhiWsResp req = new JinzhiWsResp();
        req.result = RESULT_SUCCESS;
        req.action = action;
        req.version = VERSION;
        return req;
    }

    public static JinzhiWsResp fail(String action){
        JinzhiWsResp req = new JinzhiWsResp();
        req.result = RESULT_FAIL;
        req.action = action;
        return req;
    }
}
