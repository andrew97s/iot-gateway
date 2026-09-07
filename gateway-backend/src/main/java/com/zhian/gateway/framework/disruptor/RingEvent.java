package com.zhian.gateway.framework.disruptor;

import java.io.Serializable;
import java.util.Map;

/**
 * 每个队列存储的要被执行的事件
 *
 * @author zhian
 * @since 2022-09-01
 */
public class RingEvent implements Serializable {

    /**
     * 存储的业务数据。 例如：消费的json字符串
     */
    private Object data;

    /**
     * 放入队列的时间戳
     */
    private long time;

    /**
     * 需要传递的扩展业务参数
     */
    private Map<?,?> params;

    /**
     * 服务处理类
     */
    private Class<? extends RingEventRunnable<?>> ringEventRunnableClass;


    public RingEvent() {

    }

    public RingEvent(Object data, Class<? extends RingEventRunnable<?>> ringEventRunnableClass) {
        this.data = data;
        this.ringEventRunnableClass = ringEventRunnableClass;
    }


    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Map<?,?> getParams() {
        return params;
    }

    public void setParams(Map<?,?> params) {
        this.params = params;
    }

    public Class<? extends RingEventRunnable<?>> getRingEventRunnableClass() {
        return ringEventRunnableClass;
    }

    public void setRingEventRunnableClass(Class<? extends RingEventRunnable<?>> ringEventRunnableClass) {
        this.ringEventRunnableClass = ringEventRunnableClass;
    }

    /**
     * 清空当前队列事件中的引用,防止内存长时间引用,造成堆内存溢出
     */
    public void clear(){
        this.data = null;
        this.params = null;
        this.time = 0;
        this.ringEventRunnableClass = null;

    }
}
