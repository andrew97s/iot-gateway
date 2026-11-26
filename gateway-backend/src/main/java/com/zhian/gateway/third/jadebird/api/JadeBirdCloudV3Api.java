package com.zhian.gateway.third.jadebird.api;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.third.jadebird.JadebirdCloudV3Handler;
import com.zhian.gateway.third.jadebird.vo.JadeBirdMsgV3;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 接收青鸟云平台报送，异步进行处理
 */
@Api(tags = "对接青鸟云平台V3")
@RestController
@RequestMapping("jbCloudV3")
@Slf4j
public class JadeBirdCloudV3Api {
    @Autowired
    protected JadebirdCloudV3Handler jadebirdCloudHandler;

    /**
     * 警情报送
     *
     * @param msg 告警消息
     * @return
     */
    @PostMapping("push")
    public String receiveMsg(@RequestBody String msg) {
        log.info("~~~~~~接收到[青鸟云V3]告警信息 msg: {}", msg);
        String results = "Y";
        JadeBirdMsgV3 monitorMsg = JSONObject.parseObject(msg, JadeBirdMsgV3.class);
        try {
            if (Objects.equals(monitorMsg.getCmd(), JadeBirdMsgV3.Cmd.TEST_API.getCode())) {
                //接口有效性
                return monitorMsg.getEcho();
            }
            else if (
                    Objects.equals(monitorMsg.getCmd(), JadeBirdMsgV3.Cmd.INSERT.getCode()) ||
                            Objects.equals(monitorMsg.getCmd(), JadeBirdMsgV3.Cmd.DEVICE_STATUS_CHANGE.getCode()) ||
                            Objects.equals(monitorMsg.getCmd(), JadeBirdMsgV3.Cmd.ALARM_HANDLING.getCode())
            ) {
                jadebirdCloudHandler.processMsg(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("JBV3 消息处置失败:{}" , e.getMessage());
            results = "N";
        }
        return results;
    }

}
