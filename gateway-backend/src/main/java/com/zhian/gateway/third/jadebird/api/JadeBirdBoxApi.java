package com.zhian.gateway.third.jadebird.api;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.ServletUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.vo.BoxDeviceInfo;
import com.zhian.gateway.third.jadebird.JadebirdBoxHandler;
import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 青鸟云盒http推送接口
 *
 * @author yepanpan
 */
@RestController
@RequestMapping("jbox")
@Slf4j
public class JadeBirdBoxApi
{
    @Autowired
    private IZaSysMessageService zaSysMessageService;
    @Autowired
    protected IZaSysErrorService zaSysErrorService;
    @Autowired
    private JadebirdBoxHandler jadebirdBoxHandler;

    /**
     * 云合告警
     * @param timestamp
     * @param signature
     * @param id
     * @param msg
     */
    @RequestMapping("/alarmInfo")
    public R alarmInfo(@RequestHeader("x-timestamp") String timestamp,
                                @RequestHeader("x-signature") String signature,
                                @RequestHeader("x-id") String id,
                                @RequestBody String msg) {
        log.info("----接收到[青鸟云盒]告警消息 BoxAlarmInfo: {}",  msg);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_ALARM);
        boxMsg.setMsg(msg);
        processMsg(boxMsg);
        return R.error(0, "告警成功");
    }

    /**
     * 云盒注册
     * @param timestamp
     * @param signature
     * @param id
     * @param msg
     */
    @RequestMapping("/registerDevice")
    public R registerDevice(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature, @RequestHeader("x-id") String id, @RequestBody String msg){
        log.info("box register msg: {}", msg);

        BoxDeviceInfo deviceInfo = JSONObject.parseObject(msg, BoxDeviceInfo.class);
        deviceInfo.setIp(ServletUtils.getRequest().getRemoteHost());
        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_REGISTER);
        boxMsg.setMsg(msg);
        processMsg(boxMsg);
        return R.error(0, "注册成功");
    }

    /**
     * 云盒心跳
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/heartbeat")
    public R heartbeat(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature, @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.debug("box heartbeat msg: {}", msgStr);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_HEARTBEAT);
        boxMsg.setMsg(msgStr);
        processMsg(boxMsg);
        return R.error(0, "操作成功");
    }


    /**
     * 云盒心跳
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/registerAllCamera")
    public R registerAllCamera(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature, @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.debug("box registerAllCamera msg: {}", msgStr);

        BoxMsg boxMsg = new BoxMsg();
        boxMsg.setType(BoxMsg.TYPE_REGISTER_CAMERA);
        boxMsg.setMsg(msgStr);
        processMsg(boxMsg);
        return R.error(0, "操作成功");
    }

    /**
     * 统一消息处理流程
     * @param boxMsg
     */
    private void processMsg(BoxMsg boxMsg){
        jadebirdBoxHandler.processMsg(boxMsg);
    }

}
