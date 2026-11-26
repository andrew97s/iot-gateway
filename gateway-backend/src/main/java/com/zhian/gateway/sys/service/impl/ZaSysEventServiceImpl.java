package com.zhian.gateway.sys.service.impl;

import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysEvent;
import com.zhian.gateway.sys.mapper.ZaSysEventMapper;
import com.zhian.gateway.sys.service.IZaSysEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 告警事件Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Service
public class ZaSysEventServiceImpl implements IZaSysEventService 
{

    @Autowired
    private ZaSysEventMapper zaSysEventMapper;


    /**
     * 查询告警事件
     * 
     * @param id 告警事件主键
     * @return 告警事件
     */
    @Override
    public ZaSysEvent selectZaSysEventById(Long id)
    {
        return zaSysEventMapper.selectZaSysEventById(id);
    }

    /**
     * 查询告警事件列表
     * 
     * @param zaSysEvent 告警事件
     * @return 告警事件
     */
    @Override
    public List<ZaSysEvent> selectZaSysEventList(ZaSysEvent zaSysEvent)
    {
        return zaSysEventMapper.selectZaSysEventList(zaSysEvent);
    }

    /**
     * 新增告警事件
     * 
     * @param zaSysEvent 告警事件
     * @return 结果
     */
    @Override
    public int insertZaSysEvent(ZaSysEvent zaSysEvent)
    {
        zaSysEvent.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysEvent.setCreateTime(DateUtils.getNowDate());
        return zaSysEventMapper.insertZaSysEvent(zaSysEvent);
    }

    /**
     * 修改告警事件
     * 
     * @param zaSysEvent 告警事件
     * @return 结果
     */
    @Override
    public int updateZaSysEvent(ZaSysEvent zaSysEvent)
    {
        zaSysEvent.setUpdateTime(DateUtils.getNowDate());
        return zaSysEventMapper.updateZaSysEvent(zaSysEvent);
    }

    /**
     * 批量删除告警事件
     * 
     * @param ids 需要删除的告警事件主键
     * @return 结果
     */
    @Override
    public int deleteZaSysEventByIds(Long[] ids)
    {
        return zaSysEventMapper.deleteZaSysEventByIds(ids);
    }

    /**
     * 删除告警事件信息
     * 
     * @param id 告警事件主键
     * @return 结果
     */
    @Override
    public int deleteZaSysEventById(Long id)
    {
        return zaSysEventMapper.deleteZaSysEventById(id);
    }

    @Override
    public void purge() {
        zaSysEventMapper.purge();
    }
}
