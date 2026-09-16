package com.zhian.gateway.framework.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.zhian.gateway.common.utils.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 环形队列事件处理类
 */
public class RingEventHandler implements EventHandler<RingEvent>, WorkHandler<RingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RingEventHandler.class);

    /**
     * 存储各种不同的事件处理类对应的实例,减少gc
     */
    private static Map<Class,RingEventRunnable> eventHandlerInstanceMap = new HashMap<>();

    @Override
    public void onEvent(RingEvent event, long sequence, boolean endOfBatch) throws Exception {
        this.onEvent(event);
    }

    @Override
    public void onEvent(RingEvent event) throws Exception {
//        logger.info("Element data: {}", event.getData());
//        logger.info("处理延时: {}ms", (System.currentTimeMillis() - event.getTime()));
        try {
            RingEventRunnable ringEventRunnable = eventHandlerInstanceMap.get(event.getRingEventRunnableClass());
            if (ringEventRunnable == null) {
                synchronized (event.getRingEventRunnableClass()) {
                    ringEventRunnable = eventHandlerInstanceMap.get(event.getRingEventRunnableClass());
                    if (ringEventRunnable == null) {
                        ringEventRunnable = event.getRingEventRunnableClass().newInstance();
                        eventHandlerInstanceMap.put(event.getRingEventRunnableClass(), ringEventRunnable);
                    }
                }
            }
            ringEventRunnable.execute(event);
        } catch (Exception e) {
            logger.error("环线队列处理业务{}发生异常:{}", event.getRingEventRunnableClass(), ExceptionUtil.getExceptionMessage(e));
        }
        event.clear();
    }
}


