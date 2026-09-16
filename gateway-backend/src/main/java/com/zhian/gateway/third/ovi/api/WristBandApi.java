package com.zhian.gateway.third.ovi.api;

import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.third.ovi.handler.OviPlatformHandler;
import com.zhian.gateway.third.ovi.vo.OviData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 欧孚手环API
 *
 * @author tongwenjin
 * @since 2024/8/2
 */

@RestController
@Slf4j
@RequestMapping("/api/ovi/band")
public class WristBandApi {

    @Autowired
    private OviPlatformHandler handler;

    @RequestMapping("/receive")
    public R receive(@RequestParam Map<String , String> request) throws IOException {
        log.info("接收自欧孚推送数据:{}", JSON.toJSONString(request));
        // 解析推送数据
        OviData ovidData = OviData.newInstance(request);
        if (ovidData != null) {
            // 异步处理数据
            CompletableFuture
                    .runAsync(() -> handler.processMsg(request))
                    .whenComplete((res, exp) -> {
                        if (exp != null) {
                            log.error("异步处理欧孚数据发生异常", exp);
                        }
                    });
        }

        return R.ok(ovidData != null ? "OK" : "数据非法!");
    }
}
