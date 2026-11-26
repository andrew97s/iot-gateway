package com.zhian.gateway.sys.mapper;

import com.zhian.gateway.sys.domain.ZaSysMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZaSysMessageLogMapper {

    int insert(ZaSysMessageLog row);

    List<ZaSysMessageLog> selectByMessageId(@Param("messageId") Long messageId);

    int deleteByMessageId(@Param("messageId") Long messageId);

    int deleteByMessageIds(@Param("ids") Long[] ids);

    int deleteAll();
}
