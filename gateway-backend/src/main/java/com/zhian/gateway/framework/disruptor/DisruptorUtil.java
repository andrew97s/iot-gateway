package com.zhian.gateway.framework.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.zhian.gateway.common.exception.base.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * disruptorUtil工具类(高并发框架,生产和消费的关系,提供ringbuffer环状算法,比juc传统队列性能更高)
 */
public class DisruptorUtil {


    private static final Logger logger = LoggerFactory.getLogger(DisruptorUtil.class);

    /**
     * 环线队列
     */
    private static RingBuffer<RingEvent> ringBuffer;

    /**
     * 环线队列大小
     */
    private static long bufferSize;

    private static Disruptor<RingEvent> disruptor;

    /**
     * 往队列里面放一个(事件)元素,如果放满了会阻塞
     * 使用方法：DisruptorUtil.push(new RingEvent("abc", TestRunnableTask.class));
     * 自定义TestRunnableTask类,并且实现execute方法
     *
     * @param ringEvent ringEvent
     */
    public static void push(RingEvent ringEvent) {
        if (ringBuffer == null) {
            throw new BaseException("未配置disruptor,暂不可用!");
        }
        if (ringEvent.getRingEventRunnableClass() == null) {
            throw new BaseException("事件处理服务RingEventRunnable必须指定!");
        }
        if (ringBuffer.remainingCapacity() < bufferSize * 0.1) {
            logger.warn("DISRUPTOR[告警]：目前队列已经小于10%!");
        }
        long seq = ringBuffer.next();
        RingEvent event = ringBuffer.get(seq);
        event.setParams(ringEvent.getParams());
        event.setTime(System.currentTimeMillis());
        event.setData(ringEvent.getData());
        event.setRingEventRunnableClass(ringEvent.getRingEventRunnableClass());
        //放入新数据更改状态进行消费
        ringBuffer.publish(seq);
    }

    /**
     * 提供 直接执行对应runnable class内容 方法 ，而无需继承 {@link RingEventRunnable}
     *
     * @param runnable runnable
     */
    public static void push(Runnable runnable) {
        RingEvent ringEvent = new RingEvent();
        ringEvent.setData(runnable);
        ringEvent.setRingEventRunnableClass(DefaultRingEventRunnable.class);
        push(ringEvent);
    }

    public static RingBuffer<RingEvent> getRingBuffer() {
        return ringBuffer;
    }

    public static void setRingBuffer(RingBuffer<RingEvent> ringBuffer) {
        DisruptorUtil.ringBuffer = ringBuffer;
    }

    public static long getBufferSize() {
        return bufferSize;
    }

    public static void setBufferSize(long bufferSize) {
        DisruptorUtil.bufferSize = bufferSize;
    }

    public static Disruptor<RingEvent> getDisruptor() {
        return disruptor;
    }

    public static void setDisruptor(Disruptor<RingEvent> disruptor) {
        DisruptorUtil.disruptor = disruptor;
    }
}
