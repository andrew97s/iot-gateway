package com.zhian.gateway.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接入消息Mapper接口
 * 
 * @author yepanpan
 * @date 2024-07-10
 */

@Mapper
public interface ZaSysMessageMapper extends BaseMapper<ZaSysMessage>
{
    /**
     * 查询接入消息
     * 
     * @param id 接入消息主键
     * @return 接入消息
     */
    public ZaSysMessage selectZaSysMessageById(Long id);

    /**
     * 查询接入消息列表
     * 
     * @param zaSysMessage 接入消息
     * @return 接入消息集合
     */
    public List<ZaSysMessage> selectZaSysMessageList(ZaSysMessage zaSysMessage);

    /**
     * 新增接入消息
     * 
     * @param zaSysMessage 接入消息
     * @return 结果
     */
    public int insertZaSysMessage(ZaSysMessage zaSysMessage);

    /**
     * 修改接入消息
     * 
     * @param zaSysMessage 接入消息
     * @return 结果
     */
    public int updateZaSysMessage(ZaSysMessage zaSysMessage);

    /**
     * 删除接入消息
     * 
     * @param id 接入消息主键
     * @return 结果
     */
    public int deleteZaSysMessageById(Long id);

    /**
     * 批量删除接入消息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteZaSysMessageByIds(@Param("ids") Long[] ids);

    /**
     * 清除表数据
     */
    void purge();

    /**
     * 清除sqlite缓存
     */
    void vacuum();

    /**
     * 按平台统计消息数量（total/sentCount/failedCount）
     */
    List<java.util.Map<String, Object>> countByPlatform();

    /**
     * 今日消息按类型统计
     */
    List<java.util.Map<String, Object>> countTodayByType();

    /**
     * 近24小时逐小时消息趋势
     */
    List<java.util.Map<String, Object>> selectHourlyTrend();

    /**
     * 今日推送/告警汇总
     */
    java.util.Map<String, Object> selectTodaySendStats();
}
