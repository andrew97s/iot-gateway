package com.zhian.gateway.framework.disruptor;

import lombok.extern.slf4j.Slf4j;

/**
 * @author tongwenjin
 * @since 2022/6/20
 */

@Slf4j
public class DefaultRingEventRunnable implements RingEventRunnable<Runnable> {

    @Override
    public void execute(RingEvent ringEvent) {
        Runnable runnable = (Runnable) ringEvent.getData();

        if (runnable != null) {
            runnable.run();
        } else {
            log.error("Failed to execute {} , for null runnable class !" , ringEvent);
        }
    }

}
