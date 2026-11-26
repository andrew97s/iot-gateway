package com.zhian.gateway.third.dahua.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ICC平台通用参数
 */
@Data
public class IccDataParam {
    private Map<String, Object> data = new HashMap<>();

    public static IccDataParam create(String key, Object v){
        IccDataParam param = new IccDataParam();
        param.set(key, v);
        return param;
    }

    public IccDataParam set(String key, Object v){
        data.put(key, v);
        return this;
    }
}
