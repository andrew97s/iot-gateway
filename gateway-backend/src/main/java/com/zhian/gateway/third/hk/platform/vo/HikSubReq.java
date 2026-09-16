package com.zhian.gateway.third.hk.platform.vo;

import cn.hutool.core.util.ReflectUtil;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 海康平台订阅事件请求
 *
 * @author tongwenjin
 * @since 2024 /12/11
 */
@Data
public class HikSubReq {

    private String eventDest;

    private Integer subType = 0;

    private List<Integer> eventLvl;

    private List<Integer> eventTypes;

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
