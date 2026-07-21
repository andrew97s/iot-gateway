package com.zhian.gateway.third.jadebird.api;

import cn.hutool.extra.servlet.ServletUtil;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.jadebird.JadebirdDetectorHandler;
import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import com.zhian.gateway.third.jadebird.vo.DetectorMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


/**
 * 青瞳http推送接口
 *
 * @author yepanpan
 */
@RestController
@RequestMapping("/jbDetector/v2")
@Slf4j
public class JadeBirdDetectorApi
{
    @Autowired
    protected IZaSysErrorService zaSysErrorService;
    @Autowired
    protected IZaSysMessageService zaSysMessageService;
    @Autowired
    private JadebirdDetectorHandler jadebirdDetectorHandler;
    @Autowired
    private HttpServletRequest request;

    /**
     * 告警
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/alarm")
    public R alarmInfo(@RequestHeader("x-timestamp") String timestamp,
                                @RequestHeader("x-signature") String signature,
                                @RequestHeader("x-id") String id,
                                @RequestBody String msgStr) {
        log.info("----接收到[青瞳]告警消息：timestamp: {}, signature： {}, AlarmInfo: {}", timestamp, signature, msgStr);

        DetectorMsg msg = new DetectorMsg();
        msg.setType(DetectorMsg.TYPE_ALARM);
        msg.setMsg(msgStr);
        processMsg(msg);

        return R.error(0, "成功");
    }

    /**
     * 取消告警
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/cancel-alarm")
    public R cancelAlarmInfo(@RequestHeader("x-timestamp" )String timestamp,
                          @RequestHeader("x-signature") String signature,
                          @RequestHeader("x-id") String id,
                          @RequestBody String msgStr) {
        log.info("----接收到[青瞳]告警取消消息 AlarmInfo: {}", msgStr);


        DetectorMsg msg = new DetectorMsg();
        msg.setType(DetectorMsg.TYPE_CANCEL_ALARM);
        msg.setMsg(msgStr);
        processMsg(msg);
        return R.error(0, "成功");
    }

    /**
     * 青瞳主机注册
     * {
     * "deviceId": "50:EB:F6:3E:5C:4E",
     * "ip": "172.20.20.30"
     * }
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @PostMapping("/register-device")
    public R registerDevice(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature,
                               @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.info("detector register msg: {}", msgStr);

        DetectorMsg msg = new DetectorMsg();
        msg.setType(DetectorMsg.TYPE_REGISTER_DEVICE);
        msg.setMsg(msgStr);
        processMsg(msg);
        return R.error(0, "成功");
    }


    /**
     * 青瞳主机注册
     * {
     * "deviceId": "50:EB:F6:3E:5C:4E",
     * "ip": "172.20.20.30"
     * }
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @PostMapping("/register-detector-list")
    public R registerDetectorList(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature,
                               @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.info("detector list register  msg: {}", msgStr);

        DetectorMsg msg = new DetectorMsg();
        msg.setType(DetectorMsg.TYPE_REGISTER_DETECTOR_LIST);
        msg.setMsg(msgStr);
        processMsg(msg);
        return R.error(0, "成功");
    }

    /**
     * 青瞳心跳
     * @param timestamp
     * @param signature
     * @param id
     * @param msgStr
     */
    @RequestMapping("/heartbeat")
    public R heartbeat(@RequestHeader("x-timestamp") String timestamp, @RequestHeader("x-signature") String signature, @RequestHeader("x-id") String id, @RequestBody String msgStr){
        log.debug("detector heartbeat msg: {}", msgStr);

        DetectorMsg msg = new DetectorMsg();
        msg.setType(DetectorMsg.TYPE_HEARTBEAT);
        msg.setMsg(msgStr);

        //心跳
        processMsg(msg);

        return R.error(0, "成功");
    }

    /**
     * 统一消息处理流程
     * @param boxMsg
     */
    private void processMsg(DetectorMsg boxMsg){
        String results = "Y";
        int log = 0;
        try{
            // 设置请求源IP
            boxMsg.setIp(ServletUtil.getClientIP(request));
            jadebirdDetectorHandler.processMsg(boxMsg);
        }catch (Exception e) {
            e.printStackTrace();
            log = zaSysErrorService.log(ZaSysError.TYPE_DATA, jadebirdDetectorHandler.getPlatform(), "消息处理失败: " + e.getMessage(), boxMsg.getMsg());
            results = "N";
        }
        ZaSysDevice device = MessageUtil.getDevice();
        if(device != null){
            //心跳数据不记录
            zaSysMessageService.log(device, BoxMsg.TYPE_REGISTER, boxMsg.getMsg(), results);
            MessageUtil.clear();
        }else if(!boxMsg.getType().equalsIgnoreCase(DetectorMsg.TYPE_HEARTBEAT) &&log == 0){
            zaSysErrorService.log(ZaSysError.TYPE_MQ, jadebirdDetectorHandler.getPlatform(), "消息被忽略", boxMsg.getMsg());
        }
    }

}
