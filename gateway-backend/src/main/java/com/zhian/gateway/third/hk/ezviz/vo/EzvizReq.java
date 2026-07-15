package com.zhian.gateway.third.hk.ezviz.vo;

import cn.hutool.core.util.ReflectUtil;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 萤石云基础请求
 *
 * @author tongwenjin
 * @since 2024 -11-26
 */
@Data
public class EzvizReq {

    /**
     * The Access token.
     */
    private String accessToken;

    /**
     * 将当前对象转换为map对象
     *
     * @return the request as map
     */
    public Map<String, Object> getRequestAsMap() {
        Object nullObject = new Object();
        return Arrays
                .stream(ReflectUtil.getFields(this.getClass()))
                .collect(
                        Collectors.toMap(Field::getName, field -> {
                            field.setAccessible(true);
                            try {
                                Object value = field.get(this);
                                return value != null ? value :nullObject ;
                            } catch (IllegalAccessException e) {
                                return null;
                            }
                        })
                )
                // 过滤 null 属性
                .entrySet().stream()
                .filter(entry-> !nullObject.equals(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ;
    }
}
