package com.zhian.gateway.framework.disruptor;


import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.JsonMapperUtils;
import com.zhian.gateway.common.utils.reflect.ReflectUtils;

/**
 * 环形队列被线程池处理的类
 *
 * @param <T> the type parameter
 * @author zhian
 * @since 2022-08-25
 */
@SuppressWarnings("unchecked")
public interface RingEventRunnable<T> {

    /**
     * Execute.
     *
     * @param ringEvent the ring event
     */
    void execute(RingEvent ringEvent);

    /**
     * 根据当前泛型类型 自动获取RingEvent对象内包含的数据
     *
     * @param ringEvent the ring event
     * @return the data
     */
    default T getData(RingEvent ringEvent) {
        T msgData;

        Class<?> dataClass;
        try {
            dataClass = ReflectUtils.getClassGenericType(this.getClass());
        } catch (ClassNotFoundException e) {
            dataClass = null;
        }
        if (dataClass == null) {
            throw new RuntimeException(String.format("当前类: %s , 获取泛型class失败 ！" , this.getClass()));
        }

        Object data = ringEvent.getData();
        if (data instanceof String) {
            msgData = (T) JsonMapperUtils.nonEmptyMapper().fromJson((String) data , dataClass);
        }
        else if (dataClass.equals(data.getClass())) {
            msgData = (T) data;
        } else {
            throw new ServiceException("RingRunnable消息格式错误: " + data);
        }

        if (msgData == null) {
            throw new ServiceException("RingRunnable消息为空 !");
        }

        return msgData;
    }
}
