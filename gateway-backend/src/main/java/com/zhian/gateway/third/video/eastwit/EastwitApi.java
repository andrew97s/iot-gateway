package com.zhian.gateway.third.video.eastwit;

import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.framework.manager.AsyncManager;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.TimerTask;

@RestController
@RequestMapping("/api/gateway/video")
@Slf4j
public class EastwitApi {
    @Autowired
    private EwVideoGatewayHandler ewVideoGatewayHandler;

    @ApiOperation("接收视频网关摄像头信息")
    @PostMapping("/receive")
    public AjaxResult receive(@RequestBody String msg) {
        log.info("接收到东智视频网关摄像头信息, msg: {}", msg);
        // 处理接收事件消息后的业务流程
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                ewVideoGatewayHandler.processMsg(msg);
            }
        });
        return AjaxResult.success();
    }
}
