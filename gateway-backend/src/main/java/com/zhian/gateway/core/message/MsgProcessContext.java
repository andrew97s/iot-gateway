package com.zhian.gateway.core.message;

import cn.hutool.core.collection.CollUtil;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        List<Message> msg = getMsg();
        ProcessInfo processInfo = threadLocal.get();
        if (processInfo == null) {
            log.error("添加消息失败,context未初始化!");
            return;
        }
        msg = CollUtil.isEmpty(msg) ? new ArrayList<>() : msg;
        msg.addAll(msgList);

        processInfo.setMsgList(msg);
    }

    public static List<Message> getMsg() {
        return threadLocal.get().getMsgList();
    }

    public static ProcessInfo start(Object source, ZaSysPlatform platform) {
        ProcessInfo process = new ProcessInfo();

        process.setStartTime(new Date());
        process.setPlatform(platform);
        process.setContent(source != null ? source.toString() : "");

        threadLocal.set(process);
        return process;
    }

    public static ProcessInfo finishAndGet() {
        ProcessInfo processInfo = threadLocal.get();
        processInfo.setEndTime(new Date());

        return processInfo;
    }
}
