package com.zhian.gateway.framework.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.zhian.gateway.common.utils.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * disruptor启动配置项
 */
@Configuration
@ConfigurationProperties(prefix = "zhian.disruptor")
public class DisruptorConfig {
    private static final Logger logger = LoggerFactory.getLogger(DisruptorConfig.class);

    /**
     * 事件处理线程池大小
     */
    @Value("${zhian.disruptor.threads:10}")
    private int threads;

    /**
     * 环形队列大小 默认1024*1024 = 1048576 注意，最好设置2的N次方的大小
     */
    @Value("${zhian.disruptor.bufferSize:1048576}")
    private int bufferSize;

    /**
     * 创建环形队列ringBuffer对象
     *
     * @return 环形队列ringBuffer 单例对象
     */
    @Bean
    public RingBuffer<RingEvent> createRingBuffer() {
        logger.info("----create ringBuffer----");
        ThreadFactory threadFactory = new DisruptorThreadFactory();

        // Executors.newFixedThreadPool(2, threadFactory)
        Disruptor<RingEvent> disruptor = new Disruptor<>(
                RingEvent::new,
                // 环形队列长度
                bufferSize,
                // 线程工厂
                threadFactory,
                // 多消费者类型
                ProducerType.MULTI,
                // 阻塞等待策略
                new BlockingWaitStrategy()
        );

        // 默认异常处理器
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<Object>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, Object event) {
                logger.error(
                        "disruptor exception ===> process data error sequence ==[{}] event==[{}] ,ex ==[{}]",
                        sequence,
                        event.toString(),
                        ExceptionUtil.getExceptionMessage(ex)
                );
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                logger.error("disruptor exception ===> start disruptor error ==[{}]!", ExceptionUtil.getExceptionMessage(ex));
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                logger.error("disruptor exception ===> shutdown disruptor error ==[{}]!", ExceptionUtil.getExceptionMessage(ex));
            }
        });

        // 消费者 （默认Disruptor内部线程池一个workHandler对应一个线程）
        List<RingEventHandler> consumerList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            consumerList.add(new RingEventHandler());
        }

        disruptor.handleEventsWithWorkerPool(consumerList.toArray(new RingEventHandler[0]));

        // 获取核心对象 ringBuffer 环形队列 & 开启消费者线程
        RingBuffer<RingEvent> ringBuffer = disruptor.start();

        DisruptorUtil.setBufferSize(bufferSize);
        DisruptorUtil.setRingBuffer(ringBuffer);
        DisruptorUtil.setDisruptor(disruptor);

        return ringBuffer;
    }

    /**
     * Disruptor线程工厂类
     */
    private static class DisruptorThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        private DisruptorThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "DisruptorPool-thread-";
        }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group,
                    r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()){
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
