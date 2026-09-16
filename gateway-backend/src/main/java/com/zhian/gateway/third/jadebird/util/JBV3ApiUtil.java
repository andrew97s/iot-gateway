package com.zhian.gateway.third.jadebird.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.framework.cache.CaffeineCache;
import com.zhian.gateway.framework.config.properties.CacheProperties;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.jadebird.vo.JaderBirdFacilityV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 青鸟云API相关工具类
 *
 * @author tongwenjin
 * @since 2024 /8/2
 */
@Component
@Slf4j
public class JBV3ApiUtil {

    @Autowired
    private Cache cache;

    // Token缓存key
    private final static String KEY_TOKEN = "jbf_v3_token";
    // 字典缓存key
    private final static String KEY_DICT = "jbf_v3_dict:";
    // 设备缓存key
    private final static String KEY_FACILITY = "jbf_v3_facility:";
    /**
     * The constant host.
     */
    private final String host = "http://fire.jbufacloud.com";
    /**
     * The Fire unit id.
     */
    private String appKey;
    /**
     * The Ticket.
     */
    private String appSecret;

    // 字典类型枚举
    public enum DictionaryType {
        // 设备类型
        FACILITY_TYPE("facility_type"),
        // 设备型号
        FACILITY_MODEL("facility_model"),
        // 事件类型
        STATE_TYPE("state_type"),
        // 事件
        STATE("state"),
        // 命令
        COMMAND("command"),
        // 传感器类型
        SENSOR("sensor"),
        // 模拟量单位
        METER_UNIT("meter_unit"),
        //信号
        TELECOM("telecom"),
        //系统
        SYS("sys"),

        ;
        private final String value;

        DictionaryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 设备类型
     */
    public enum JBFType {
        JBF_VS10N("JBF-VS10N", "无线感烟探测器", "火灾报警系统"),
        JF_VS10N("JF-DY101N", "无线感烟探测器", "火灾报警系统"),
        JBF_VS51N("JBF-VS51N", "无线可燃气体探测器", "可燃气体系统"),
        ;
        private String code;
        private String name;
        private String sys;

        JBFType(String code, String name, String sys) {
            this.code = code;
            this.name = name;
            this.sys = sys;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getSys() {
            return sys;
        }
    }


    public void start(ZaSysPlatform platform) {
        platform.setConfig(platform.getConfig());
        this.appSecret = platform.getConfigStr("appSecret");
        this.appKey = platform.getConfigStr("appKey");
        // 构造时加载关键字典
        loadCriticalDictionaries();
    }

    public void loadCriticalDictionaries() {
        for (DictionaryType value : DictionaryType.values()) {
            Map<String, String> dict = getDict(value.getValue(), 3);
            if (StringUtils.isNotEmpty(dict)) {
                log.info("加载青鸟云V3字典成功：{}", value.getValue());
            } else {
                log.error("加载关键字典失败：{}", value.getValue());
            }
        }
    }


    /**
     * 执行消音操作
     */
    public void mute(String code, int errorNum) {
        JaderBirdFacilityV3 byAddr = getByAddr(code, true, 2);
        Map<String, String> commandMap = getDict(DictionaryType.COMMAND.getValue(), 3);
        if (StringUtils.isEmpty(commandMap)) {
            throw new ServiceException("执行消音失败未获取到命令");
        }
        String command = commandMap.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue(), "消音"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (StringUtils.isBlank(command)) {
            throw new ServiceException("执行消音失败,消音命令不存在");
        }
        // 消音请求参数
        Map<String, Object> map = new HashMap<>();
        map.put("id", byAddr.getId());
        map.put("command", command);
        map.put("manual", 1);
        map.put("auto", 1);
        sleep();
        String endpoint = String.format("%s/api/open/v3/facility/control", host);
        String response = sendPostRequest(endpoint, JSONObject.toJSONString(map), getHeaders());
        log.info("青鸟云V3反控接口返回:{},参数:{}", response, map);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (Objects.equals(jsonObject.getInteger("code"), 908) && errorNum > 0) {
            log.error("token失效");
            cache.deleteObject(KEY_TOKEN);
            mute(code, errorNum - 1);
        }
    }

    /**
     * 添加设备
     */
    public JaderBirdFacilityV3 addFacility(String imei, String descr, String model, Integer errorNum) {
        JaderBirdFacilityV3 hasFacility = getByAddr(imei, false, 3);
        if (Objects.nonNull(hasFacility)) {
            log.info("设备已经在青鸟云V3添加：{}", imei);
            return hasFacility;
        }
        //获取字典
        Map<String, String> modelMap = getDict(DictionaryType.FACILITY_MODEL.getValue(), 3);
        //信号
        Map<String, String> telecomMap = getDict(DictionaryType.TELECOM.getValue(), 3);
        //系统
        Map<String, String> sysMap = getDict(DictionaryType.SYS.getValue(), 3);
        sleep();
        String endpoint = String.format("%s/api/open/v3/facility", host);
        JaderBirdFacilityV3 addFacilitiesVo = new JaderBirdFacilityV3();
        String modelCode = modelMap.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue(), model))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("1");
        String telCode = telecomMap.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue(), "电信NB-IoT"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("1");
        String sysCode = getSysCode(sysMap, model);
        addFacilitiesVo.setDescr(descr);
        addFacilitiesVo.setModelCode(Long.parseLong(modelCode));
        addFacilitiesVo.setAddr(imei);
        addFacilitiesVo.setTelecomCode(Long.parseLong(telCode));
        addFacilitiesVo.setSysCode(sysCode == null ? null : Long.parseLong(sysCode));
        String jsonString = JSONObject.toJSONString(addFacilitiesVo);
        Map<String, String> headers = getHeaders();
        String response = sendPostRequest(endpoint, jsonString, headers);
        log.info("青鸟云V3添加设备结果:{},headers:{},参数:{}", response, headers, addFacilitiesVo);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (Objects.equals(jsonObject.getInteger("code"), 908) && errorNum > 0) {
            log.error("token失效");
            cache.deleteObject(KEY_TOKEN);
            return addFacility(imei, descr, model, errorNum - 1);
        } else if (jsonObject.getInteger("code") == 1) {
            JSONArray data = jsonObject.getJSONArray("data");
            return JSONObject.parseObject(JSONObject.toJSONString(data.getJSONObject(0)), JaderBirdFacilityV3.class);
        }
        throw new ServiceException("添加设备失败" + jsonObject.getString("message"));

    }


    /**
     * Description: 根据编码地址获取设备详情
     *
     * @return
     * @date 2024/12/2 11:47
     * @Param
     */
    public JaderBirdFacilityV3 getByAddr(String code, boolean queryCache, int errorNum) {
        String key = KEY_FACILITY + code;
        JaderBirdFacilityV3 cacheMap = cache.getCacheObject(key);
        if (Objects.nonNull(cacheMap) && queryCache) {
            return cacheMap;
        }
        sleep();
        String endpoint = String.format("%s/api/open/v3/facility/getByAddr?addr=%s", host, code);
        String response = sendGetRequest(endpoint, getHeaders());
        log.debug("查询设备信息:{}", response);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (jsonObject == null) {
            return null;
        }
        if (Objects.equals(jsonObject.getInteger("code"), 908) && errorNum > 0) {
            log.error("token失效");
            cache.deleteObject(KEY_TOKEN);
            return getByAddr(code, queryCache, errorNum - 1);
        }
        if (!Objects.equals(jsonObject.getInteger("code"), 1)) {
            throw new ServiceException("获取设备失败");
        }
        JSONArray data = jsonObject.getJSONArray("data");
        JaderBirdFacilityV3 facilityV3 = JSONObject.parseObject(JSONObject.toJSONString(data.getJSONObject(0)), JaderBirdFacilityV3.class);
        if (facilityV3 == null || facilityV3.getId() == null) {
            return null;
        }
        cache.setCacheObject(key, facilityV3, 5, TimeUnit.MINUTES);
        return facilityV3;
    }

    /**
     * 获取请求头参数
     * message:传输数据
     * getToken：是否需要获取token
     */
    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Token", getToken());
        headers.put("Content-Type", "application/json");
        return Collections.unmodifiableMap(headers);
    }

    //青鸟云获取V3Token
    private String getToken() {
        String cacheToken = cache.getCacheObject(KEY_TOKEN);
        if (StrUtil.isNotBlank(cacheToken)) {
            return cacheToken;
        }
        sleep();
        String endpoint = String.format("%s/api/open/v3/auth/token?appKey=%s&appSecret=%s&refresh=%s", host, appKey, appSecret, false);
        String response = HttpUtil.get(endpoint);
        log.info("青鸟云获取Token结果:{}", response);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!Objects.equals(jsonObject.getInteger("code"), 1)) {
            throw new ServiceException("获取Token失败");
        }
        long tokenExpire = jsonObject.getJSONObject("data").getLong("tokenExpire");
        String token = jsonObject.getJSONObject("data").getString("accessToken");
        if (tokenExpire > 0) {
            tokenExpire = tokenExpire - System.currentTimeMillis() / 1000;
        } else {
            throw new ServiceException("获取Token失败");
        }
        cache.setCacheObject(KEY_TOKEN, token, (int) tokenExpire, TimeUnit.SECONDS);
        return token;
    }

    //获取字典
    public Map<String, String> getDict(String dictKey, int errorNum) {
        String cacheKey = KEY_DICT + dictKey;
        Map<String, String> cacheMap = cache.getCacheMap(cacheKey);
        if (cacheMap != null && StringUtils.isNotEmpty(cacheMap)) {
            return cacheMap;
        }
        String endpoint = String.format("%s/api/open/v3/dict/list", host);
        JSONObject requestBody = new JSONObject();
        requestBody.put("category", dictKey);
        sleep();
        String response = sendPostRequest(endpoint, requestBody.toJSONString(), getHeaders());
        log.info("青鸟云字典结果:{},字典:{}", response, dictKey);
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (Objects.equals(jsonObject.getInteger("code"), 908) && errorNum > 0) {
            log.error("token失效");
            cache.deleteObject(KEY_TOKEN);
            return getDict(dictKey, errorNum - 1);
        }
        if (!Objects.equals(jsonObject.getInteger("code"), 1)) {
            throw new ServiceException("获取字典失败");
        }
        Map<String, String> map = new HashMap<>();
        jsonObject.getJSONArray("data").forEach(item -> {
            JSONObject json = JSONObject.parseObject(item.toString());
            if (Objects.equals(json.getInteger("enabled"), 1)) {
                map.put(json.getString("code"), json.getString("name"));
            }
        });
        cache.setCacheMap(cacheKey, map);
        cache.expire(cacheKey, 1 + RandomUtil.randomInt(0, 10), TimeUnit.DAYS);
        return map;
    }


    /**
     * 发送 POST 请求
     */
    private String sendPostRequest(String url, String body, Map<String, String> headers) {
        return HttpRequest.post(url)
                .headerMap(headers, true)
                .body(body)
                .execute()
                .body();
    }

    /**
     * 发送 GET 请求
     */
    private String sendGetRequest(String url, Map<String, String> headers) {
        return HttpRequest.get(url)
                .headerMap(headers, true)
                .execute()
                .body();
    }

    /**
     * Description: 获取系统类型
     *
     * @date 2024/12/6 9:59
     * @Param []
     */
    private String getSysCode(Map<String, String> sysMap, String model) {
        return Arrays.stream(JBFType.values())
                .filter(jbfType -> Objects.equals(model, jbfType.getName()) || Objects.equals(model, jbfType.getCode()))
                .map(JBFType::getSys)
                .flatMap(sysName -> sysMap.entrySet().stream()
                        .filter(entry -> Objects.equals(entry.getValue(), sysName))
                        .map(Map.Entry::getKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Description:查询接口需要等待防止出发流控
     * 目前不止查询设备1秒一次，全部接口需要1秒，后续等待青鸟云修正再做处理
     *
     * @return void
     * @date 2024/12/6 10:23
     * @Param []
     */
    private void sleep() {
        try {
            // 接口需要确保至少间隔1秒钟，防止触发流控
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
