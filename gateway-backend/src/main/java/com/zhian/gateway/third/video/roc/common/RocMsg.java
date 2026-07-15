package com.zhian.gateway.third.video.roc.common;

import com.alibaba.fastjson2.JSON;
import lombok.Data;

/**
 * ROC api请求
 *
 * @author tongwenjin
 * @since 2025-2-18
 */

@Data
public class RocMsg {

    private String method;

    private String uri;

    private Auth auth;

    private Object body;

    public static RocMsg create(String method, String uri){
        RocMsg msg = new RocMsg();
        msg.method = method;
        msg.uri = uri;
        return msg;
    }

    public static RocMsg create(String method, String uri, Object body){
        RocMsg msg = new RocMsg();
        msg.method = method;
        msg.uri = uri;
        msg.body = body;
        return msg;
    }

    @Data
    public static class Auth {
        private String algorithm;

        private String cnonce;

        private String nc;

        private String nonce;

        private String nextnonce;

        private String qop;

        private String realm;

        private String response;

        private String username;
    }

    public String toJsonStr() {
        return JSON.toJSONString(this);
    }
}
