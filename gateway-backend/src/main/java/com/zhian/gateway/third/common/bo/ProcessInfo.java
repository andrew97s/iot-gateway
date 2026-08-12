package com.zhian.gateway.third.common.bo;

import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 处理信息
 *
 * @author tongwenjin
 * @since 2026/7/30
 */

@Data
public class ProcessInfo {

    // 处理开始时间
    private Date startTime;

    // 处理结束时间
    private Date endTime;

    // 插件配置
    private ZaSysPlatform platform;

    // 设备
    private ZaSysDevice device;

    // 消息类型
    private String type;

    // 处理状态
    private String handleStatus;

    // 处理结果
    private String handleResult;

    // 消息内容
    private String content;

    // 处理结果对象(推送上级平台)
    private List<Message> msgList;

    // 转换后的统一消息（JSON，多条时为JSON数组）
    private String unifiedContent;

    // 各上级平台推送结果（记录消息日志用）
    private List<MessageSyncHandler.UpstreamPushResult> pushResults;

    public static ProcessInfo newInstance(ZaSysDevice device, String content, String type , List<MqMessage> msgList) {
        if (device == null) {
            return null;
        }

        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus(Constants.YES);
        info.setHandleResult("OK");
        info.setType(type);
        info.setContent(content);
//        info.setMsgList(msgList);
        return info;
    }

    public static ProcessInfo newAlarm(ZaSysDevice device, String content , List<MqMessage> msgList) {
        if (device == null) {
            return null;
        }

        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus("Y");
        info.setHandleResult("OK");
        info.setType(MsgConstants.MSG_TYPE_ALARM);
        info.setContent(content);
//        info.setMsgList(msgList);
        return info;
    }

    public static ProcessInfo newBusiness(ZaSysDevice device, String content , List<MqMessage> msgList) {
        if (device == null) {
            return null;
        }

        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus("Y");
        info.setHandleResult("OK");
        info.setType(MsgConstants.MSG_TYPE_BUSINESS);
        info.setContent(content);
//        info.setMsgList(msgList);
        return info;
    }

    public static ProcessInfo newError(ZaSysDevice device, String content, String errorMsg) {
        if (device == null) {
            return null;
        }

        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus("N");
        info.setHandleResult(errorMsg);
        info.setType(MsgConstants.MSG_TYPE_ALARM);
        info.setContent(content);
        return info;
    }

    public static ProcessInfo newDevice(ZaSysDevice device, String content , List<MqMessage> msgList) {
        if (device == null) {
            return null;
        }

        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus("Y");
        info.setHandleResult("ok");
        info.setType(MsgConstants.MSG_TYPE_DEVICE);
        info.setContent(content);
//        info.setMsgList(msgList);
        return info;
    }

    public static ProcessInfo newControl(ZaSysDevice device, ControlVo controlVo, R result) {
        if (device == null) {
            return null;
        }

        boolean success = result != null && Objects.equals(result.get("code"), 200);
        ProcessInfo info = new ProcessInfo();
        info.setDevice(device);
        info.setHandleStatus(success ? "1" : "0");
        info.setHandleResult(JSON.toJSONString(result));
        info.setType(MsgConstants.MSG_TYPE_CONTROL);
        info.setContent(JSON.toJSONString(controlVo));
        return info;
    }

}
