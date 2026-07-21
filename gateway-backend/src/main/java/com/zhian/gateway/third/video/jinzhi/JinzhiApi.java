package com.zhian.gateway.third.video.jinzhi;

import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


/**
 * 金智网关注册接口
 *
 * @author yepanpan
 */
@RestController
@RequestMapping("jz")
@Slf4j
public class JinzhiApi
{

    /**
     * 设备注册
     */
    @GetMapping("/register")
    public R alarmInfo(HttpServletRequest request
            , @RequestParam(value = "ip" , required = false) String ip
            , @RequestParam(value = "port" , required = false) Integer port) {
        R result = R.success();
        ip = StrUtil.isNotBlank(ip) ? ip : request.getLocalAddr();
        if(port == null){
            port = request.getServerPort();
        }
        result.put("webSocketAddress", "ws://" + ip + ":" + port + "/jz/ws");
        result.put("recordAddress", "http://" + ip + ":" + port + "/jz/UploadPictureRecording");
        return result;
    }

}
