package com.zhian.gateway.third.video.jinzhi.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * 请求对象
 */
@Data
public class JinzhiWsReq {
    private String version;
    private String action;
    private String param;

    public JSONObject getObject(){
        return JSONObject.parseObject(param);
    }

}
