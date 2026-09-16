package com.zhian.gateway.sys.utils;

import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import lombok.Data;

import java.util.List;

/**
 * 接收消息的处理工具
 */

public class MessageUtil {
    private static ThreadLocal<ZaSysDevice> deviceThreadLocal = new ThreadLocal<>();


    private static ThreadLocal<List<Message>> msgThreadLocal = new ThreadLocal<>();

    public static void setDevice(ZaSysDevice zaSysDevice){
        deviceThreadLocal.set(zaSysDevice);
    }

    public static ZaSysDevice getDevice(){
        return deviceThreadLocal.get();
    }

    public static void addMsg(Message message){
        msgThreadLocal.get().add(message);
    }

    public static List<Message> getMsg(){
        return msgThreadLocal.get();
    }

    public static void clear(){
        deviceThreadLocal.remove();
        msgThreadLocal.remove();
    }
}
