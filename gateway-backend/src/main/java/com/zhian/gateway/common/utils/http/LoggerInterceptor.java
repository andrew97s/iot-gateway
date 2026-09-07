package com.zhian.gateway.common.utils.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * RestTemplate 日志拦截器
 *
 * @author tongwenjin
 * @since 2022/11/10
 */

@Slf4j
public class LoggerInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        long startTime = System.currentTimeMillis();
        StringBuilder headersString = new StringBuilder();
        request.getHeaders().forEach((key , value)-> headersString.append(key).append(":").append(value).append(";"));

        // 记录请求参数
        log.debug("Send {} request to : {}", request.getMethod() , request.getURI().toURL());
        log.debug("Request headers : {}" , headersString);
        log.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);

        headersString.setLength(0);
        response.getHeaders().forEach((key, value) -> headersString.append(key).append(":").append(value).append(";"));
        log.debug("Response headers : {}", headersString);

        // 包装一层 实现 响应inputStream 重复读取
        String subType = response.getHeaders().getContentType().getSubtype();
        if(subType.contains("text") || subType.contains("json") || subType.contains("html")) {
            response = new HttpResponseMessageWrapper(response);
            InputStream is = response.getBody();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);

            // 记录响应参数

            String responseBody = new BufferedReader(isr).lines()
                    .collect(Collectors.joining("\n"));
            log.debug("Response body: {}", responseBody);

            // 重置流
            is.reset();
        }else{
            log.debug("响应结果的二制流不展示");
        }
        log.debug("Finished request in {} ms, length: {}", System.currentTimeMillis() - startTime, response.getHeaders().getContentLength());
        return response;
    }
}
