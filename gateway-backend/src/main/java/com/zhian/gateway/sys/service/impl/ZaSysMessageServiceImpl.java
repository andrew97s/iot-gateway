package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysMessageLog;
import com.zhian.gateway.sys.mapper.ZaSysMessageLogMapper;
import com.zhian.gateway.sys.mapper.ZaSysMessageMapper;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import com.zhian.gateway.third.gw.MessageSyncHandler.PushTargetSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 接入消息Service业务层处理
 * 
 * @author yepanpan
 * @date 2024-07-10
 */
@Slf4j
@Service
public class ZaSysMessageServiceImpl extends ServiceImpl<ZaSysMessageMapper , ZaSysMessage> implements IZaSysMessageService
{
    @Autowired
    private ZaSysMessageMapper zaSysMessageMapper;

    @Autowired
    private ZaSysMessageLogMapper pushLogMapper;

    /**
     * 快速记录错误日志
     * @param device 设备
     * @param type 类别
     * @param content 数据内容
     * @param results 结果
     */
    @Override
    public ZaSysMessage log(ZaSysDevice device, String type, String content, String results){
        // 忽略心跳数据
        if ("heartbeat".equals(type)){
            return null;
        }

        ZaSysMessage zaSysMessage = new ZaSysMessage();
        zaSysMessage.setCreateTime(new Date());
        zaSysMessage.setContent(content);
        zaSysMessage.setType(type);
        zaSysMessage.setDeviceId(device.getId());
        zaSysMessage.setDeviceCode(device.getCode());
        zaSysMessage.setHandleStatus(results);
        zaSysMessage.setPfCode(device.getPfCode());
        zaSysMessageMapper.insertZaSysMessage(zaSysMessage);
        return zaSysMessage;
    }

    @Override
    public ZaSysMessage log(ProcessInfo info) {
        // 忽略心跳数据
        if ("heartbeat".equals(info.getType())){
            return null;
        }

        ZaSysDevice device = info.getDevice();

        ZaSysMessage message = new ZaSysMessage();
        message.setCreateTime(new Date());
        message.setContent(info.getContent());
        message.setType(info.getType());
        message.setDeviceId(device.getId());
        message.setDeviceCode(device.getCode());
        message.setHandleStatus(info.getHandleStatus());
        message.setHandleResult(info.getHandleResult());
        message.setPfCode(device.getPfCode());
        zaSysMessageMapper.insertZaSysMessage(message);
        return message;
    }

    /**
     * 查询接入消息
     * 
     * @param id 接入消息主键
     * @return 接入消息
     */
    @Override
    public ZaSysMessage selectZaSysMessageById(Long id)
    {
        return zaSysMessageMapper.selectZaSysMessageById(id);
    }

    /**
     * 查询接入消息列表
     * 
     * @param zaSysMessage 接入消息
     * @return 接入消息
     */
    @Override
    public List<ZaSysMessage> selectZaSysMessageList(ZaSysMessage zaSysMessage)
    {
        return zaSysMessageMapper.selectZaSysMessageList(zaSysMessage);
    }

    /**
     * 新增接入消息
     * 
     * @param zaSysMessage 接入消息
     * @return 结果
     */
    @Override
    public int insertZaSysMessage(ZaSysMessage zaSysMessage)
    {
        zaSysMessage.setId(SnowflakeIdWorker.getInstance().nextId());
        zaSysMessage.setCreateTime(DateUtils.getNowDate());
        return zaSysMessageMapper.insertZaSysMessage(zaSysMessage);
    }

    /**
     * 修改接入消息
     * 
     * @param zaSysMessage 接入消息
     * @return 结果
     */
    @Override
    public int updateZaSysMessage(ZaSysMessage zaSysMessage)
    {
        return zaSysMessageMapper.updateZaSysMessage(zaSysMessage);
    }

    /**
     * 批量删除接入消息
     * 
     * @param ids 需要删除的接入消息主键
     * @return 结果
     */
    @Override
    public int deleteZaSysMessageByIds(Long[] ids)
    {
        if (ids != null && ids.length > 0) {
            pushLogMapper.deleteByMessageIds(ids);
        }
        return zaSysMessageMapper.deleteZaSysMessageByIds(ids);
    }

    /**
     * 删除接入消息信息
     *
     * @param id 接入消息主键
     * @return 结果
     */
    @Override
    public int deleteZaSysMessageById(Long id)
    {
        pushLogMapper.deleteByMessageId(id);
        return zaSysMessageMapper.deleteZaSysMessageById(id);
    }

    @Override
    public void purge() {
        pushLogMapper.deleteAll();
        zaSysMessageMapper.purge();
    }

    @Override
    public List<ZaSysMessageLog> selectPushLogsByMessageId(Long messageId) {
        return pushLogMapper.selectByMessageId(messageId);
    }

    @Override
    public boolean retryMessage(Long id) {
        ZaSysMessage msg = zaSysMessageMapper.selectZaSysMessageById(id);
        if (msg == null || msg.getContent() == null) {
            return false;
        }
        MessageSyncHandler messageSyncHandler = SpringUtils.getBean("messageSyncHandler", MessageSyncHandler.class);
        PushTargetSnapshot meta = messageSyncHandler.currentPushTarget()
                .orElse(new PushTargetSnapshot("unknown", "网关不可用"));

        ZaSysMessageLog pushLog = new ZaSysMessageLog();
        pushLog.setId(SnowflakeIdWorker.getInstance().nextId());
        pushLog.setMessageId(id);
        pushLog.setType(meta.pushMode);
        pushLog.setTarget(truncate(meta.targetAddress, 1900));
        pushLog.setTime(new Date());

        boolean ok;
        try {
            messageSyncHandler.processMsg(msg.getContent());
            ok = true;
        } catch (Exception e) {
            log.error("消息[{}]推送失败", id, e);
            ok = false;
            pushLog.setFailReason(truncate(e.getMessage(), 950));
        }
        pushLog.setStatus(ok ? ZaSysMessageLog.STATUS_SUCCESS : ZaSysMessageLog.STATUS_FAILURE);
        if (ok) {
            pushLog.setFailReason(null);
        } else if (pushLog.getFailReason() == null) {
            pushLog.setFailReason("未知错误");
        }
        try {
            pushLogMapper.insert(pushLog);
        } catch (Exception ex) {
            log.warn("写入推送记录失败 messageId={}", id, ex);
        }

        ZaSysMessage update = new ZaSysMessage();
        update.setId(id);
        update.setSendStatus(ok ? ZaSysMessage.SEND_STATUS_SENT : ZaSysMessage.SEND_STATUS_FAILED);
        update.setRetryCount((msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1);
        update.setRetryTime(new Date());
        zaSysMessageMapper.updateZaSysMessage(update);
        return ok;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * 按平台统计消息数量
     *
     * @return list of {pfCode, total, sentCount, failedCount}
     */
    @Override
    public List<Map<String, Object>> countByPlatform() {
        return zaSysMessageMapper.countByPlatform();
    }

    @Override
    public List<Map<String, Object>> countTodayByType() {
        return zaSysMessageMapper.countTodayByType();
    }

    @Override
    public List<Map<String, Object>> selectHourlyTrend() {
        return zaSysMessageMapper.selectHourlyTrend();
    }

    @Override
    public Map<String, Object> selectTodaySendStats() {
        return zaSysMessageMapper.selectTodaySendStats();
    }

    @Override
    public List<ZaSysMessage> selectLatestAlarms(int limit) {
        com.github.pagehelper.PageHelper.startPage(1, limit, "id desc");
        ZaSysMessage query = new ZaSysMessage();
        query.setType("alarm");
        return zaSysMessageMapper.selectZaSysMessageList(query);
    }
}
