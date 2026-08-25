package com.zhian.gateway.core.message;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息处置上下文对象 , 每个插件从消息处置开始阶段，调用当前start
 *
 * @author tongwenjin
 * @since 2026/7/30
 */
@Slf4j
public class MsgProcessContext {

    private static ThreadLocal<ProcessInfo> threadLocal = new ThreadLocal<>();


    public static ProcessInfo getProcessInfo() {
        return threadLocal.get();
    }

    public static void setProcessInfo(ProcessInfo processInfo) {
        threadLocal.set(processInfo);
    }


    public static void addMsg(Message message) {
        addMsg(Collections.singletonList(message));
    }

    public static void addMsg(List<Message> msgList) {
        ProcessInfo processInfo = threadLocal.get();
        if (processInfo == null) {
            log.error("添加消息失败,context未初始化!");
            return;
        }

        List<Message> msg = getMsg();
        msg = CollUtil.isEmpty(msg) ? new ArrayList<>() : msg;
        msgList = msgList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        msg.addAll(msgList);

        // 填充设备信息
        if (processInfo.getDevice() == null && CollUtil.isNotEmpty(msgList)) {
            IZaSysDeviceService deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
            processInfo.setDevice(deviceService.selectZaSysDeviceById(msgList.get(0).getDevice().getDeviceId()));
        }

        processInfo.setMsgList(msg);
    }

    public static List<Message> getMsg() {
        return threadLocal.get().getMsgList();
    }

    public static ProcessInfo start(Object source, ZaSysPlatform platform) {
        ProcessInfo process = new ProcessInfo();

        process.setStartTime(new Date());
        process.setPlatform(platform);
        process.setContent(source instanceof String ? (String) source : JSON.toJSONString(source));

        threadLocal.set(process);
        return process;
    }

    public static ProcessInfo failed(String reason) {
        ProcessInfo processInfo = threadLocal.get();
        processInfo.setEndTime(new Date());
        processInfo.setHandleStatus("0");
        processInfo.setHandleResult(reason);

        return processInfo;
    }

    public static ProcessInfo finishAndGet() {
        ProcessInfo processInfo = threadLocal.get();
        processInfo.setEndTime(new Date());

        return processInfo;
    }
}
