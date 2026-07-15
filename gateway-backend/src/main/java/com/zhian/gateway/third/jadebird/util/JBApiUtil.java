package com.zhian.gateway.third.jadebird.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 青鸟云API相关工具类
 *
 * @author tongwenjin
 * @since 2024 /8/2
 */
@Slf4j
public class JBApiUtil {

    /**
     * The constant host.
     */
    private static final String host = "http://push.jbufacloud.com/jbfsys";
    /**
     * The Fire unit id.
     */
    private final String fireUnitId;
    /**
     * The Ticket.
     */
    private final String ticket;
    /**
     * The Token.
     */
    private String token;

    /**
     * The Token expire time.
     */
    private Long tokenExpireTime;

    /**
     * Instantiates a new Jb api util.
     *
     * @param platform the platform
     */
    public JBApiUtil(ZaSysPlatform platform) {

        platform.setConfig(platform.getConfig());

        this.fireUnitId = platform.getConfigStr("fireUnitId");
        this.ticket = platform.getConfigStr("ticket");
    }

    /**
     * Instantiates a new Jb api util.
     *
     * @param fireUnitId the fire unit id
     * @param ticket     the ticket
     */
    public JBApiUtil(String fireUnitId , String ticket) {
        this.fireUnitId = fireUnitId;
        this.ticket = ticket;
    }

    /**
     * 执行消音操作
     * <a href="http://jbf-push.apifox.cn/api-57099275">文档地址</a>
     *
     * @param facilityId   the facility id
     * @param facilityCode the facility code
     */
    public void mute(String facilityId, String facilityCode) {
        // 消音请求参数
        Map<String, Object> map = new HashMap<>();
        map.put("fireUnitId", fireUnitId);
        map.put("facilitiesId", facilityId);
        map.put("facilitiesCode", facilityCode);

        // 拼接参数 （青鸟云文档显示为body参数 ， 但是实际使用还是需要query参数 ！！）
        String url = "/api/facilities/mute?facilitiesCode=2&facilitiesId=" + facilityId;

        post(AuthType.TOKEN, url, JSON.toJSONString(map));
    }

    /**
     * 启动联动
     *
     * @param facilityId   the facility id
     * @param facilityCode the facility code
     */
    public void startLinkage(String facilityId, String facilityCode) {
        this.linkage(facilityId, facilityCode, 2);
    }

    /**
     * 停止联动
     *
     * @param facilityId   the facility id
     * @param facilityCode the facility code
     */
    public void stopLinkage(String facilityId, String facilityCode) {
        this.linkage(facilityId, facilityCode, 4);
    }

    /**
     * 远程联动.
     * <a href="http://jbf-push.apifox.cn/api-74168084">文档地址</a>
     *
     * @param facilityId   the facility id
     * @param facilityCode the facility code
     * @param type         操作类型，2：启动；4：停止
     */
    private void linkage(String facilityId, String facilityCode, Integer type) {
        // 消音请求参数
        Map<String, Object> map = new HashMap<>();
        map.put("fireUnitId", fireUnitId);
        map.put("facilitiesId", facilityId);
        map.put("facilitiesCode", facilityCode);
        map.put("type", type);

        post(AuthType.TOKEN, "/api/facilities/mute", JSON.toJSONString(map));
    }


    /**
     * Dict stat type.
     */
    public void dictStatType() {
        get(AuthType.SIGNATURE, "/api/dict/statType");
    }

    /**
     * Dict stat.
     */
    public void dictStat() {
        get(AuthType.SIGNATURE, "/api/dict/stat");
    }

    /**
     * 获取token
     * <a href="http://jbf-push.apifox.cn/api-74279328">文档地址</a>
     */
    private void token() {
        JSONObject jsonObject = get(AuthType.SIGNATURE, "/api/token");
        this.token = jsonObject.getJSONObject("data").getString("token");
        this.tokenExpireTime = jsonObject.getJSONObject("data").getLong("tokenExpiry");
    }

    /**
     * Refresh token string.
     *
     * @return the string
     */
    public String refreshToken() {
        long offset = (tokenExpireTime != null ? tokenExpireTime : 0L) - System.currentTimeMillis();

        // token 为空 或 过期时间小于 3 min 执行刷新token操作
        if (StrUtil.isBlank(token) || offset < 1000 * 60 * 3) {
            this.token();
        }

        return token;
    }

    /**
     * Signature string.
     *
     * @param message   the message
     * @param timestamp the timestamp
     * @param ticket    the ticket
     * @return the string
     */
    private String signature(String message, String timestamp, String ticket) {
        try {
            if (StrUtil.isNotBlank(message)) {
                message = message + timestamp + ticket;
            } else {
                message = timestamp + ticket;
            }

            MessageDigest instance = MessageDigest.getInstance("SHA-1");
            instance.reset();
            instance.update(message.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : instance.digest()) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        } catch (Exception ignore) {
            return "";
        }
    }

    /**
     * Get json object.
     *
     * @param authType the auth type
     * @param url      the url
     * @return the json object
     */
    public JSONObject get(AuthType authType, String url) {
        return get(authType, url, null);
    }

    /**
     * Get json object.
     *
     * @param authType the auth type
     * @param url      the url
     * @param params   the params
     * @return the json object
     */
    public JSONObject get(AuthType authType, String url, Map<String, Object> params) {
        if (null != params) {
            url = url + "?";
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                url = url + (entry.getKey() + "=" + entry.getValue() + "&");
            }

            url = url.substring(0 , url.length() - 1);
        }

        return request(HttpMethod.GET, authType, url, "");
    }

    /**
     * Post json object.
     *
     * @param authType the auth type
     * @param url      the url
     * @param payload  the payload
     * @return the json object
     */
    public JSONObject post(AuthType authType, String url, String payload) {
        return request(HttpMethod.POST, authType, url, payload);
    }

    /**
     * Request json object.
     *
     * @param method   the method
     * @param authType the auth type
     * @param url      the url
     * @param payload  the payload
     * @return the json object
     */
    private JSONObject request(HttpMethod method, AuthType authType, String url, String payload) {

        String timestamp;
        // 成签名
        String signature;
        if (StrUtil.isNotBlank(ticket)) {
            timestamp = String.valueOf(System.currentTimeMillis());
            signature = signature(payload, timestamp, ticket);
        } else {
            timestamp = "";
            signature = "";
        }

        HttpRequest request = HttpUtil.createRequest(Method.GET, host + url);
        if (HttpMethod.POST == method) {
            request = HttpUtil.createRequest(Method.POST, host + url);
            request.header("Content-Type" , "application/json; charset=utf-8");
        }
        request.header("Fire-Unit-Id", fireUnitId);
        if (AuthType.TOKEN == authType) {
            // token鉴权
            request.header("Token", refreshToken());
            log.info("{} {}, Token:{}, Payload:{}", method, host + url, token, payload);
        } else if (AuthType.SIGNATURE == authType) {
            // 签名鉴权
            request.header("X-Timestamp", timestamp);
            request.header("X-Signature", signature);
            log.info("{} {}, X-Signature:{}, X-Timestamp:{}, Payload:{}", method, url, signature, timestamp, payload);
        }

        try (HttpResponse response = request.execute()){
            String body = response.body();
            log.debug("Response {}", body);
            return JSONObject.parseObject(body);
        } catch (Exception e) {
            log.error("服务异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The enum Http method.
     */
    public enum HttpMethod {
        /**
         * Get http method.
         */
        GET,
        /**
         * Post http method.
         */
        POST
    }

    /**
     * The enum Auth type.
     */
    public enum AuthType {
        /**
         * Token auth type.
         */
        TOKEN,
        /**
         * Signature auth type.
         */
        SIGNATURE
    }
}
