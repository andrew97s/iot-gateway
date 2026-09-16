package com.zhian.gateway.framework.datasource.mp;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zhian.gateway.common.core.domain.BaseEntity;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * 字段填充拦截器（兼容mybatis）
 *
 * @author tongwenjin
 * @since 2024-8-15
 */

@Intercepts(
        {
                @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
        }
)
@Component
public class MybatisFieldInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameterObject = invocation.getArgs()[1];
        String sqlCommandType = mappedStatement.getSqlCommandType().name();

        // 填充ID
        if ("INSERT".equals(sqlCommandType)) {
            Field[] fields = ReflectUtil.getFieldsDirectly(parameterObject.getClass() , false);
            for (Field field : fields) {
                field.setAccessible(true);
                // baseEntity 子类
                if (parameterObject instanceof BaseEntity && ((BaseEntity)(parameterObject)).getId() == null) {
                    ((BaseEntity)(parameterObject)).setId(SnowflakeIdWorker.getInstance().nextId());
                }
                if (field.isAnnotationPresent(TableId.class) && field.get(parameterObject) == null) {
                    field.set(parameterObject , SnowflakeIdWorker.getInstance().nextId());
                    break;
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
