/*******************************************************************************
 * Copyright (c) 2005, 2014 springside.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *******************************************************************************/
package com.zhian.gateway.common.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

/**
 * 简单封装Jackson，实现JSON String<->Java Object转换的Mapper.
 * <p>
 * 可以直接使用公共示例JsonMapper.INSTANCE, 也可以使用不同的builder函数创建实例，封装不同的输出风格,
 * <p>
 * 不要使用GSON, 在对象稍大时非常缓慢.
 * <p>
 * 注意: 需要参考本模块的POM文件，显式引用jackson.
 *
 * @author calvin
 */
public class JsonMapperUtils {
    /**
     * 日志
     */
    private static Logger logger = LoggerFactory.getLogger(JsonMapperUtils.class);
    /**
     *
     */
    public static final JsonMapperUtils INSTANCE = new JsonMapperUtils();
    /**
     *
     */
    private ObjectMapper mapper;
    /**
     * 只输出不为null
     */
    private static JsonMapperUtils nonNullMapper = new JsonMapperUtils(Include.NON_NULL);

    private static JsonMapperUtils nonNullMapperNoLong2Str = new JsonMapperUtils(Include.NON_NULL,false);
    /**
     * 只输出不为空
     */
    private static JsonMapperUtils nonEmptyMapper = new JsonMapperUtils(Include.NON_EMPTY);
    /**
     * 所有字段
     */
    private static JsonMapperUtils defaultMapper = new JsonMapperUtils();

    public JsonMapperUtils() {
        this(null);
    }

    public JsonMapperUtils(Include include) {
        this(include,true);
    }

    public JsonMapperUtils(Include include, Boolean longToStr) {
        mapper = new ObjectMapper();
        // 设置输出时包含属性的风格
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }

        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        //将字符串转换日期时转换成中国时区
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        if(longToStr){
            SimpleModule xssModule = new SimpleModule();
            xssModule.addSerializer(Long.class, ToStringSerializer.instance);
            xssModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            mapper.registerModule(xssModule);
        }
    }

    /**
     * 创建只输出非Null的属性到Json字符串的Mapper.
     */
    public static JsonMapperUtils nonNullMapper() {
        return nonNullMapper;
    }

    /**
     * 创建只输出非Null且非Empty(如List.isEmpty)的属性到Json字符串的Mapper.
     * <p>
     * 注意，要小心使用, 特别留意empty的情况.
     */
    public static JsonMapperUtils nonEmptyMapper() {
        return nonEmptyMapper;
    }

    public static JsonMapperUtils getNonNullMapperNoLong2Str(){
        return nonNullMapperNoLong2Str;
    }

    /**
     * 默认的全部输出的Mapper, 区别于INSTANCE，可以做进一步的配置
     */
    public static JsonMapperUtils defaultMapper() {
        return defaultMapper;
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。 如果对象为Null, 返回"null". 如果集合为空集合, 返回"[]".
     */
    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            logger.warn("write to json string error:" + object, e);
            return null;
        }
    }

    /**
     * 反序列化POJO或简单Collection如List<String>.
     * <p>
     * 如果JSON字符串为Null或"null"字符串, 返回Null. 如果JSON字符串为"[]", 返回空集合.
     * <p>
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(String, JavaType)
     *
     * @see #fromJson(String, JavaType)
     */
    public <T> T fromJson(@NotNull String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return mapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化复杂Collection如List<Bean>, contructCollectionType()或contructMapType()构造类型, 然后调用本函数.
     *
     */
    public <T> T fromJson(@NotNull String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return (T) mapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化复杂的带有泛型的对象
     *    RestResponse<VeOpenResponse> object = JsonMapper.nonNullMapper().fromJson(json, new TypeReference<RestResponse<VeOpenResponse>>() {});
     * @see
     */
    public <T> T fromJson(@NotNull String jsonString, TypeReference typeReference) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return (T) mapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            logger.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 构造Collection类型.
     */
    public JavaType buildCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return mapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    /**
     * 构造Map类型.
     */
    public JavaType buildMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
    }

    /**
     * 当JSON里只含有Bean的部分属性時，更新一個已存在Bean，只覆盖該部分的属性.
     */
    public void update(String jsonString, Object object) {
        try {
            mapper.readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException e) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        } catch (IOException e) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", e);
        }
    }

    /**
     * 輸出JSONP格式數據.
     */
    public String toJsonP(String functionName, Object object) {
        return toJson(new JSONPObject(functionName, object));
    }

    /**
     * 設定是否使用Enum的toString函數來讀寫Enum, 為False時時使用Enum的name()函數來讀寫Enum, 默認為False. 注意本函數一定要在Mapper創建後, 所有的讀寫動作之前調用.
     */
    public void enableEnumUseToString() {
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    /**
     * 取出Mapper做进一步的设置或使用其他序列化API.
     */
    public ObjectMapper getMapper() {
        return mapper;
    }
}

