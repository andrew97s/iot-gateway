package com.zhian.gateway.framework.datasource.mp;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.zhian.gateway.common.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * update insert 自动填充 (仅适用于mp)
 *
 * @author tongwenjin
 * @since 2024 -8-14
 */
@Component
public class ZaMetaObjectHandler implements MetaObjectHandler {
    /**
     * insert语句填充
     *
     * @param metaObject metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        fillData(metaObject, "createTime", Date.class, new Date());
        fillData(metaObject, "createBy", Long.class, SecurityUtils.getUserId());
        fillData(metaObject, "updateTime", Date.class, new Date());
        fillData(metaObject, "updateBy", Long.class, SecurityUtils.getUserId());
    }

    /**
     * update语句填充
     *
     * @param metaObject metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        fillData(metaObject, "updateTime", Date.class, new Date());
        fillData(metaObject, "updateBy", Long.class, SecurityUtils.getUserId());
    }

    private <T, E extends T> void fillData(MetaObject metaObject, String fieldName, Class<T> fieldType, E fieldVal) {
        if (metaObject.hasSetter(fieldName)) {
            this.setFieldValByName(fieldName, fieldVal, metaObject);
        }
    }
}
