package com.zhian.gateway.third.common.context;

import com.zhian.gateway.third.common.bo.ProcessInfo;

/**
 * 消息处理上下文对象
 *
 * @author tongwenjin
 * @since 2026/5/12
 */
public class ProcessContext {

    private static ThreadLocal<ProcessInfo> processThreadLocal = new ThreadLocal<>();

    public static void set(ProcessInfo info) {
        processThreadLocal.set(info);
    }

    public static ProcessInfo get() {
        return processThreadLocal.get();
    }

    public static void clear() {
        processThreadLocal.remove();
    }
}
