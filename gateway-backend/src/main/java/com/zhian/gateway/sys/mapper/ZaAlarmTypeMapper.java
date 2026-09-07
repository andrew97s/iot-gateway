package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标准告警类型 Mapper
 */
@Mapper
public interface ZaAlarmTypeMapper extends BaseMapper<ZaAlarmType> {
}
