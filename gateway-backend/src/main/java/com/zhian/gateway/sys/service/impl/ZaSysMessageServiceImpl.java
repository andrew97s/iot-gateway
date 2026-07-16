package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
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
        message.setId(SnowflakeIdWorker.getInstance().nextId());
        message.setCreateTime(new Date());
        message.setContent(info.getContent());
        message.setUnifiedContent(info.getUnifiedContent());
        message.setType(info.getType());
        message.setDeviceId(device.getId());
        message.setDeviceCode(device.getCode());
        message.setHandleStatus(info.getHandleStatus());
        message.setHandleResult(info.getHandleResult());
        message.setPfCode(device.getPfCode());

        // 汇总各上级平台推送结果：全部成功=sent，任一失败=failed（失败消息可重推）
        List<MessageSyncHandler.UpstreamPushResult> results = info.getPushResults();
        if (results != null && !results.isEmpty()) {
            boolean allOk = results.stream().allMatch(MessageSyncHandler.UpstreamPushResult::isSuccess);
            message.setSendStatus(allOk ? ZaSysMessage.SEND_STATUS_SENT : ZaSysMessage.SEND_STATUS_FAILED);
        }
        zaSysMessageMapper.insertZaSysMessage(message);

        // 逐上级平台记录推送明细
        if (results != null) {
            for (MessageSyncHandler.UpstreamPushResult r : results) {
                insertPushLog(message.getId(), r);
            }
        }
        return message;
    }

    private void insertPushLog(Long messageId, MessageSyncHandler.UpstreamPushResult r) {
        try {
            ZaSysMessageLog pushLog = new ZaSysMessageLog();
            pushLog.setId(SnowflakeIdWorker.getInstance().nextId());
            pushLog.setMessageId(messageId);
            pushLog.setType(r.getPushType());
            pushLog.setTarget(truncate((r.getUpstreamName() == null ? "" : r.getUpstreamName() + " | ") + r.getTarget(), 1900));
            pushLog.setTime(new Date());
            pushLog.setStatus(r.isSuccess() ? ZaSysMessageLog.STATUS_SUCCESS : ZaSysMessageLog.STATUS_FAILURE);
            pushLog.setFailReason(r.isSuccess() ? null : truncate(r.getError() == null ? "未知错误" : r.getError(), 950));
            pushLogMapper.insert(pushLog);
        } catch (Exception ex) {
            log.warn("写入推送记录失败 messageId={}", messageId, ex);
        }
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
        if (msg == null) {
            return false;
        }
        // 优先重推统一消息（保持原 messageId，上级平台按其去重保证幂等）；无统一消息时回退原始报文
        String payload = StringUtils.isNotEmpty(msg.getUnifiedContent()) ? msg.getUnifiedContent() : msg.getContent();
        if (StringUtils.isEmpty(payload)) {
            return false;
        }
        MessageSyncHandler messageSyncHandler = SpringUtils.getBean("gatewayHandler", MessageSyncHandler.class);
        List<MessageSyncHandler.UpstreamPushResult> results = messageSyncHandler.pushToUpstreams(payload);

        boolean ok;
        if (results.isEmpty()) {
            // 未配置任何上级平台
            ok = false;
            MessageSyncHandler.UpstreamPushResult none = new MessageSyncHandler.UpstreamPushResult();
            none.setUpstreamName("-");
            none.setPushType("unknown");
            none.setTarget("未配置上级平台");
            none.setSuccess(false);
            none.setError("未配置任何启用的上级平台");
            insertPushLog(id, none);
        } else {
            ok = results.stream().allMatch(MessageSyncHandler.UpstreamPushResult::isSuccess);
            for (MessageSyncHandler.UpstreamPushResult r : results) {
                insertPushLog(id, r);
            }
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
    public List<Map<String, Object>> countTodayByPlatform() {
        return zaSysMessageMapper.countTodayByPlatform();
    }

    @Override
    public List<Map<String, Object>> countTodayByType() {
        return zaSysMessageMapper.countTodayByType();
    }

    @Override
    public List<Map<String, Object>> countTodayByTypeAndPlatform(String pfCode) {
        return zaSysMessageMapper.countTodayByTypeAndPlatform(pfCode);
    }

    @Override
    public List<Map<String, Object>> selectHourlyTrend() {
        return zaSysMessageMapper.selectHourlyTrend();
    }

    @Override
    public List<Map<String, Object>> selectHourlyTrendByPlatform(String pfCode) {
        return zaSysMessageMapper.selectHourlyTrendByPlatform(pfCode);
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
