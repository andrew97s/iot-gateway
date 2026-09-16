package com.zhian.gateway.framework.disruptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunnableTask implements RingEventRunnable {

    private static final Logger logger = LoggerFactory.getLogger(TestRunnableTask.class);

    @Override
    public void execute(RingEvent event) {
        logger.info("Element data: {}", event.getData());
        logger.info("处理延时: {}ms", (System.currentTimeMillis() - event.getTime()));
    }
}
