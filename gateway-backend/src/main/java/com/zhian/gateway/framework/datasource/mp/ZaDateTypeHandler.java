package com.zhian.gateway.framework.datasource.mp;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.DateOnlyTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 自定义日期类型转换器
 *
 * @author tongwenjin
 * @since 2024-8-28
 */
@MappedJdbcTypes(value = {JdbcType.DATE, JdbcType.TIMESTAMP}, includeNullJdbcType = true)
@MappedTypes(Date.class)
@Slf4j
public class ZaDateTypeHandler extends DateOnlyTypeHandler {

    private Boolean useSqlite = null;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
        // 针对sqlite的datetime 字段（此处直接使用string去存储）
        if (ifUseSqlite(ps)) {
            ps.setString(i, DateUtil.format(parameter, DatePattern.NORM_DATETIME_PATTERN));
        }
        else {
            if (jdbcType == JdbcType.TIMESTAMP || jdbcType == null) {
                ps.setTimestamp(i, new Timestamp(parameter.getTime()));
            }else if (jdbcType == JdbcType.DATE) {
                ps.setDate(i, new java.sql.Date(parameter.getTime()));
            } else {
                super.setNonNullParameter(ps, i, parameter, jdbcType);
            }
        }
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp sqlDate = null;
        try {
            sqlDate = rs.getTimestamp(columnName);
            if (sqlDate != null) {
                return new Date(sqlDate.getTime());
            }
        }
        // 尝试通过字符串转换
        catch (Exception ignore) {
            String dateStr = rs.getString(columnName);
            DateTime dateTime = DateUtil.parse(dateStr);
            return new  java.sql.Date(dateTime.getTime());
        }
        return null;
    }


    private boolean ifUseSqlite(PreparedStatement ps) {
        if (useSqlite != null) {
            return useSqlite;
        }
        try {
            // 根据参数class判断当前数据库连接是否为sqlite
            useSqlite = ps.getParameterMetaData().getClass().getName().contains("sqlite");
        } catch (SQLException e) {
            log.error("初始化useSqlite失败,msg:{}", e.getMessage());
            useSqlite = false;
        }
        return useSqlite;
    }
}
