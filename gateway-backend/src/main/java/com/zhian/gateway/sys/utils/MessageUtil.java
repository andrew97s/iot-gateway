package com.zhian.gateway.sys.utils;

import com.zhian.gateway.sys.domain.ZaSysDevice;

/**
 * 接收消息的处理工具
 */
public class MessageUtil {
    private static ThreadLocal<ZaSysDevice> deviceThreadLocal = new ThreadLocal<>();

    public static void setDevice(ZaSysDevice zaSysDevice){
        deviceThreadLocal.set(zaSysDevice);
    }

    public static ZaSysDevice getDevice(){
        return deviceThreadLocal.get();
    }

    public static void clear(){
        deviceThreadLocal.remove();
    }
}
