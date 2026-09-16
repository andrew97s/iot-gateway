package com.zhian.gateway.api.controller;

import com.zhian.gateway.common.core.domain.R;
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
    public R heart(){
        return R.success("网关在线");
    }
}
