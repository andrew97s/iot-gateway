package com.zhian.gateway.third.hk.platform.api;

import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.third.hk.platform.HikvisionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 海康平台对接API
 *
 * @author tongwenjin
 * @since 2024-12-11
 */

@Slf4j
@RestController
@RequestMapping("/api/hk_platform/")
public class HkPlatformApi {

    @Autowired
    private HikvisionHandler handler;

    @RequestMapping("/event_webhook")
    public String eventWebhook(@RequestBody String bodyStr) {

        log.info("接收到海康平台的事件:{}", bodyStr);

        CompletableFuture.runAsync(() -> handler.doProcessMsg(bodyStr)).exceptionally(e -> {
            log.error("处理海康事件发生异常:{}", e.getMessage());
            SpringUtils.getBean(IZaSysErrorService.class).log(ZaSysError.TYPE_MQ, handler.getPlatform(), "消息处理失败: " + e.getMessage(), bodyStr);
            return null;
        });

        return "OK";
    }
}
