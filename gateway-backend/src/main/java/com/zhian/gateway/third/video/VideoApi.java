package com.zhian.gateway.third.video;

import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 视频网关上报接口 TODO
 */
@RequestMapping("video")
@Slf4j
@Controller
public class VideoApi{

    /**
     * 心跳
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/heart")
    public void heartbeat(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature, @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.debug("box heartbeat, timestamp: {}, signature： {}, id: {}", timestamp, signature, id);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_HEARTBEAT);
        boxMsg.setMsg(msgStr);
        //jadebirdBoxHandler.processMsg(boxMsg);
    }
}
