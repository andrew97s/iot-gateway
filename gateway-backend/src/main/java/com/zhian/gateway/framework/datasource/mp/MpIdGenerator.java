package com.zhian.gateway.framework.datasource.mp;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;

/**
 * ID生成器(雪花ID)
 *
 * @author tongwenjin
 * @since 2024-8-15
 */
public class MpIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return SnowflakeIdWorker.getInstance().nextId() ;
    }
}
