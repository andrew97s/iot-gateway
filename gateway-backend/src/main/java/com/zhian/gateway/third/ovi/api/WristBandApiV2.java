package com.zhian.gateway.third.ovi.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.third.ovi.handler.BandHandlerV2;
import com.zhian.gateway.third.ovi.vo.BandDataV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 欧孚手环API
 *
 * @author tongwenjin
 * @since 2024/8/2
 */

@SuppressWarnings("CallToPrintStackTrace")
@RestController
@Slf4j
@RequestMapping("/api/open2/band")
public class WristBandApiV2 {

    @Autowired
    private BandHandlerV2 bandHandler;

    @RequestMapping("/receive")
    public R<String> receive(HttpServletRequest request) throws Exception {
        // 使用 BufferedReader 读取请求体
        String requestBody = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)
        )
                .lines()
                .collect(Collectors.joining("\n"));
        log.info("接收自OPEN2推送的body数据:{}", requestBody);
        CompletableFuture.runAsync(() -> {
            // json格式转换
            JSONObject json = JSON.parseObject(requestBody);
            String dataStr = json.getString("ResultData");
            JSONObject jsonData = JSON.parseObject(dataStr);
            json.put("ResultData", jsonData);
            BandDataV2 bandData = JSON.parseObject(json.toJSONString(), BandDataV2.class);
            bandData.setOriginalData(json.toJSONString());
            if ("alarm".equals(json.getString("DataType"))) {
                bandData.setAlarmData(JSON.parseObject(dataStr, BandDataV2.AlarmData.class));
            }
            else {
                bandData.setData(JSON.parseObject(dataStr, BandDataV2.Data.class));
            }

            bandHandler.processMsg(bandData);
        }).whenComplete((r, e) -> {
            if (e != null) {
                log.error("手环数据消费发生异常:", e);
                e.printStackTrace();
            }
        });

        return R.ok("OK");
    }
}
