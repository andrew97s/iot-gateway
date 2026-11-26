package com.zhian.gateway.third.hk.ezviz.api;

import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.third.hk.ezviz.EzvizHandler;
import com.zhian.gateway.third.hk.ezviz.vo.WebhookReq;
import com.zhian.gateway.third.hk.ezviz.vo.WebhookResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * 萤石云API
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@RestController
@RequestMapping("/api/ezviz")
@Slf4j
public class EzvizApi {

    @Autowired
    private EzvizHandler handler;
    @Autowired
    private IZaSysMessageService messageService;

    @RequestMapping("/msg")
    public WebhookResp msg(@RequestHeader HttpHeaders header, @RequestBody String body) {
        log.info("消息获取时间:{}, 请求头:{},请求体:{}", System.currentTimeMillis(), JSON.toJSONString(header), body);
        WebhookReq req = null;
        try {
            req = JSON.parseObject(body, WebhookReq.class);
            WebhookReq finalReq = req;
            CompletableFuture.runAsync(() -> handler.processMsg(finalReq))
                    .whenComplete((r,s)-> {
                        // TODO 记录日志
                    })
                    .exceptionally(e -> {
                        log.error(" 萤石云消息处理失败,msg: {}" , e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 必须进行返回
        String messageId = req != null ? req.getHeader().getMessageId() : "";
        return new WebhookResp(messageId);
    }
}
