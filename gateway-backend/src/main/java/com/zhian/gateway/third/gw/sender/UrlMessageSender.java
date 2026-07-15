package com.zhian.gateway.third.gw.sender;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * URL消息发送器：支持多推送地址
 * <p>
 * 配置方式（任选其一）：
 * 1. pushUrls = JSON 数组，如 ["http://a","http://b"]
 * 2. pushUrls = 换行/逗号分隔字符串
 * 3. pushUrl  = 单个 URL（兼容旧配置）
 * </p>
 */
@Slf4j
public class UrlMessageSender implements MessageSender {

    private final ZaSysPlatform platform;

    public UrlMessageSender(ZaSysPlatform platform) {
        this.platform = platform;
    }

    @Override
    public boolean start(ZaSysPlatform platform) {
        return false;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public void send(String message) {
        List<String> urls = resolveUrls();
        if (urls.isEmpty()) {
            log.warn("URL推送：未配置任何推送地址，消息被丢弃");
            return;
        }
        for (String url : urls) {
            try {
                log.debug("URL({})推送消息:{}", url, message);
                String response = HttpUtil.post(url, message);
                log.debug("推送完成[{}]，返回:{}", url, response);
            } catch (Exception e) {
                log.error("URL({})推送失败:{}", url, e.getMessage());
                SpringUtils.getBean(IZaSysErrorService.class)
                        .log(ZaSysError.TYPE_OTHER, "HTTP发送消息失败[" + url + "]", e.getMessage(), message);
            }
        }
    }

    /**
     * 解析推送地址列表（兼容 JSON 数组、逗号/换行分隔、单 URL 三种格式）
     */
    private List<String> resolveUrls() {
        // 优先读 pushUrls 字段（多地址）
        String pushUrls = platform.getConfigStr("pushUrls");
        if (StringUtils.isNotEmpty(pushUrls)) {
            pushUrls = pushUrls.trim();
            // JSON 数组格式
            if (pushUrls.startsWith("[")) {
                try {
                    return JSON.parseArray(pushUrls, String.class);
                } catch (Exception ignore) {}
            }
            // 换行或逗号分隔
            String[] parts = pushUrls.split("[,\n]+");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) list.add(t);
            }
            return list;
        }
        // 兼容旧配置 pushUrl（单地址）
        String pushUrl = platform.getConfigStr("pushUrl");
        return StringUtils.isNotEmpty(pushUrl) ? Collections.singletonList(pushUrl.trim()) : Collections.emptyList();
    }
}
