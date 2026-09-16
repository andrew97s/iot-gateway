package com.zhian.gateway.sys.service;

import com.zhian.gateway.sys.domain.ZaSysEvent;

import java.util.List;

/**
 * 告警事件Service接口
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
public interface IZaSysEventService 
{
    /**
     * 查询告警事件
     *
     * @param id 告警事件主键
     * @return 告警事件
     */
    public ZaSysEvent selectZaSysEventById(Long id);

    /**
     * 查询告警事件列表
     * 
     * @param zaSysEvent 告警事件
     * @return 告警事件集合
     */
    public List<ZaSysEvent> selectZaSysEventList(ZaSysEvent zaSysEvent);

    /**
     * 新增告警事件
     * 
     * @param zaSysEvent 告警事件
     * @return 结果
     */
    public int insertZaSysEvent(ZaSysEvent zaSysEvent);

    /**
     * 修改告警事件
     * 
     * @param zaSysEvent 告警事件
     * @return 结果
     */
    public int updateZaSysEvent(ZaSysEvent zaSysEvent);

    /**
     * 批量删除告警事件
     * 
     * @param ids 需要删除的告警事件主键集合
     * @return 结果
     */
    public int deleteZaSysEventByIds(Long[] ids);

    /**
     * 删除告警事件信息
     * 
     * @param id 告警事件主键
     * @return 结果
     */
    public int deleteZaSysEventById(Long id);

    /**
     * Purge.
     */
    void purge();
}
