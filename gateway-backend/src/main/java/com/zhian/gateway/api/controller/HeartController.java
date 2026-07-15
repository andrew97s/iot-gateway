package com.zhian.gateway.api.controller;

import com.zhian.gateway.common.core.domain.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heart")
public class HeartController {

    /**
     * 心跳接口
     * @return
     */
    @GetMapping("keepalive")
    public AjaxResult heart(){
        return AjaxResult.success("网关在线");
    }
}
