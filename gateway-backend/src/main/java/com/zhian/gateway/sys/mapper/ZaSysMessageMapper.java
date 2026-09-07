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
     * 查询设备最近的监测消息。
     *
     * @param deviceId 设备主键
     * @param limit 最大返回条数
     * @return 最近监测消息
     */
    List<ZaSysMessage> selectLatestTelemetry(@Param("deviceId") Long deviceId,
                                             @Param("limit") int limit);

    /**
     * 查询设备最近的告警消息。
     *
     * @param deviceId 设备主键
     * @param limit 最大返回条数
     * @return 最近告警消息
     */
    List<ZaSysMessage> selectLatestAlarms(@Param("deviceId") Long deviceId,
                                         @Param("limit") int limit);

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
     * 今日按平台统计消息数量
     */
    List<java.util.Map<String, Object>> countTodayByPlatform();

    /**
     * 今日消息按类型统计
     */
    List<java.util.Map<String, Object>> countTodayByType();

    /**
     * 指定平台今日消息按类型统计
     */
    List<java.util.Map<String, Object>> countTodayByTypeAndPlatform(@Param("pfCode") String pfCode);

    /**
     * 近24小时逐小时消息趋势
     */
    List<java.util.Map<String, Object>> selectHourlyTrend();

    /**
     * 指定平台近24小时逐小时消息趋势
     */
    List<java.util.Map<String, Object>> selectHourlyTrendByPlatform(@Param("pfCode") String pfCode);

    /**
     * 今日推送/告警汇总
     */
    java.util.Map<String, Object> selectTodaySendStats();
}
