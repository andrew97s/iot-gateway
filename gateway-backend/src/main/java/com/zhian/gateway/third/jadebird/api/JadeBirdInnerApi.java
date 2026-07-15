package com.zhian.gateway.third.jadebird.api;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.jadebird.JadebirdInnerHandler;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 接收青鸟轻量化网关推送的消息，异步进行处理
 * 
 * @author yepanpan
 */
@Api(tags = "对接青鸟轻量化网关")
@RestController
@RequestMapping("jbInner")
@Slf4j
public class JadeBirdInnerApi
{
    @Autowired
    protected IZaSysMessageService zaSysMessageService;
    @Autowired
    protected IZaSysErrorService zaSysErrorService;
    @Autowired
    private JadebirdInnerHandler jadebirdInnerHandler;

    /**
     * 警情报送
     *
     * @param msg       告警消息
     * @return
     */
    @PostMapping("push")
    public AjaxResult receiveMsg(@RequestBody String msg) {
        log.info("~~~~~~接收到[青鸟网关]告警信息,  msg: {}", msg);
        // 校验签名
//        if (!SignatureUtils.checkSignature(msg, timestamp, ticket, signature)) {
//            logger.error("签名不合法, timestamp: {}, signature： {}, msg: {}", timestamp, signature, msg);
//            throw new BaseException("签名不合法");
//        }
        // 消息处理

        String results = "Y";
        int log = 0;
        MonitorMsg monitorMsg = JSONObject.parseObject(msg, MonitorMsg.class);
        try{
            jadebirdInnerHandler.processMsg(msg);
        }catch (Exception e) {
            e.printStackTrace();
            log = zaSysErrorService.log(ZaSysError.TYPE_MQ, jadebirdInnerHandler.getPlatform(), "消息处理失败: " + e.getMessage(), msg);
            results = "N";
        }
        ZaSysDevice device = MessageUtil.getDevice();
        //记录消息
        if(device != null){
            zaSysMessageService.log(MessageUtil.getDevice(), monitorMsg.getEvent(), msg, results);
            MessageUtil.clear();
        }else if(!monitorMsg.getEvent().equalsIgnoreCase(MonitorMsg.Event.HEARTBEAT.name()) && log == 0){
            zaSysErrorService.log(ZaSysError.TYPE_MQ, jadebirdInnerHandler.getPlatform(), "消息被忽略", msg);
        }
        return AjaxResult.success();
    }

}
