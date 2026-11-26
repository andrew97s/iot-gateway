package com.zhian.gateway.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysMessageLog;
import com.zhian.gateway.sys.mapper.ZaSysMessageLogMapper;
import com.zhian.gateway.sys.mapper.ZaSysMessageMapper;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageViewHelper;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 接入消息Service业务层处理
 *
 * @author yepanpan
 * @date 2024-07-10
 */
@Slf4j
@Service
public class ZaSysMessageServiceImpl extends ServiceImpl<ZaSysMessageMapper, ZaSysMessage> implements IZaSysMessageService {
    @Autowired
    private ZaSysMessageMapper zaSysMessageMapper;

    @Autowired
    private ZaSysMessageLogMapper pushLogMapper;

    /**
     * 快速记录错误日志
     *
     * @param device  设备
     * @param type    类别
     * @param content 数据内容
     * @param results 结果
     */
    @Override
    public ZaSysMessage log(ZaSysDevice device, String type, String content, String results) {
        // 忽略心跳数据
        if ("heartbeat".equals(type)) {
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
        if (info.getStartTime() != null && info.getEndTime() != null) {
            message.setCostTime(info.getEndTime().getTime() - info.getStartTime().getTime());
        }
        if (info.getMsgList() != null && !info.getMsgList().isEmpty()) {
            String types = info.getMsgList().stream().map(Message::getType).collect(Collectors.joining(","));
            message.setType(types);
        }

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
    public ZaSysMessage selectZaSysMessageById(Long id) {
        return zaSysMessageMapper.selectZaSysMessageById(id);
    }

    /**
     * 查询接入消息列表
     *
     * @param zaSysMessage 接入消息
     * @return 接入消息
     */
    @Override
    public List<ZaSysMessage> selectZaSysMessageList(ZaSysMessage zaSysMessage) {
        prepareKeyword(zaSysMessage);
        return zaSysMessageMapper.selectZaSysMessageList(zaSysMessage);
    }

    /** 统一消息ID（UUID 等）只存在于 unified_content 里，关键字足够长时才去匹配它，避免短关键字触发大字段全表扫描 */
    private static final int KEYWORD_CONTENT_MIN_LENGTH = 16;

    /**
     * 列表关键字（params.keyword）预处理：
     * 设备编码始终模糊匹配；纯数字额外按消息主键精确匹配；长度足够的非纯数字关键字额外匹配统一消息内容。
     * keywordId / keywordInContent 一律由这里决定，不采信前端传值。
     */
    private static void prepareKeyword(ZaSysMessage query) {
        if (query == null) {
            return;
        }
        Map<String, Object> params = query.getParams();
        params.remove("keywordId");
        params.remove("keywordInContent");
        Object raw = params.get("keyword");
        String keyword = raw == null ? "" : String.valueOf(raw).trim();
        if (keyword.isEmpty()) {
            params.remove("keyword");
            return;
        }
        params.put("keyword", keyword);
        if (keyword.matches("\\d{1,19}")) {
            try {
                params.put("keywordId", Long.parseLong(keyword));
            } catch (NumberFormatException ignored) {
                // 超出 bigint 范围，不可能是消息主键
            }
        } else if (keyword.length() >= KEYWORD_CONTENT_MIN_LENGTH) {
            params.put("keywordInContent", Boolean.TRUE);
        }
    }

    /**
     * 新增接入消息
     *
     * @param zaSysMessage 接入消息
     * @return 结果
     */
    @Override
    public int insertZaSysMessage(ZaSysMessage zaSysMessage) {
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
    public int updateZaSysMessage(ZaSysMessage zaSysMessage) {
        return zaSysMessageMapper.updateZaSysMessage(zaSysMessage);
    }

    /**
     * 批量删除接入消息
     *
     * @param ids 需要删除的接入消息主键
     * @return 结果
     */
    @Override
    public int deleteZaSysMessageByIds(Long[] ids) {
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
    public int deleteZaSysMessageById(Long id) {
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
        Object success = retryMessageDetail(id, null).get("success");
        return Boolean.TRUE.equals(success);
    }

    @Override
    public Map<String, Object> retryMessageDetail(Long id, List<String> upstreamCodes) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", id);
        detail.put("success", false);
        ZaSysMessage msg = zaSysMessageMapper.selectZaSysMessageById(id);
        if (msg == null) {
            detail.put("error", "消息不存在");
            return detail;
        }
        MessageViewHelper.fillMessageIdAndSummary(msg);
        detail.put("messageId", msg.getMessageId());
        detail.put("deviceCode", msg.getDeviceCode());
        detail.put("summary", msg.getSummary());
        if ("control".equalsIgnoreCase(msg.getType())) {
            detail.put("error", "反控消息不支持重推");
            return detail;
        }
        String payload = StringUtils.isNotEmpty(msg.getUnifiedContent()) ? msg.getUnifiedContent() : msg.getContent();
        if (StringUtils.isEmpty(payload)) {
            detail.put("error", "消息内容为空");
            return detail;
        }
        List<String> codes = upstreamCodes == null ? new ArrayList<>() : new ArrayList<>(upstreamCodes);
        if (codes.isEmpty()) {
            codes.addAll(failedUpstreamCodes(id));
        }
        MessageSyncHandler messageSyncHandler = SpringUtils.getBean("gatewayHandler", MessageSyncHandler.class);
        List<MessageSyncHandler.UpstreamPushResult> results = codes.isEmpty()
                ? messageSyncHandler.pushToUpstreams(payload)
                : messageSyncHandler.pushToUpstreams(payload, codes);

        if (results.isEmpty()) {
            MessageSyncHandler.UpstreamPushResult none = new MessageSyncHandler.UpstreamPushResult();
            none.setUpstreamName("-");
            none.setPushType("unknown");
            none.setTarget("未配置上级平台");
            none.setSuccess(false);
            none.setError(codes.isEmpty() ? "未配置任何启用的上级平台" : "所选通道不存在或未启用");
            insertPushLog(id, none);
            results.add(none);
        } else {
            for (MessageSyncHandler.UpstreamPushResult r : results) {
                insertPushLog(id, r);
            }
        }

        boolean attemptOk = results.stream().allMatch(MessageSyncHandler.UpstreamPushResult::isSuccess);
        boolean allChannelsOk = latestChannelsAllSuccess(id);
        ZaSysMessage update = new ZaSysMessage();
        update.setId(id);
        update.setSendStatus(allChannelsOk ? ZaSysMessage.SEND_STATUS_SENT : ZaSysMessage.SEND_STATUS_FAILED);
        update.setRetryCount((msg.getRetryCount() == null ? 0 : msg.getRetryCount()) + 1);
        update.setRetryTime(new Date());
        zaSysMessageMapper.updateZaSysMessage(update);

        String error = results.stream()
                .filter(r -> !r.isSuccess())
                .map(r -> (r.getUpstreamName() == null ? "通道" : r.getUpstreamName()) + "：" + (r.getError() == null ? "失败" : r.getError()))
                .collect(Collectors.joining("；"));
        detail.put("success", attemptOk);
        detail.put("sendStatus", update.getSendStatus());
        detail.put("error", attemptOk ? "" : error);
        return detail;
    }

    /**
     * 最近一次推送失败的上级通道编码。日志里只存了平台名称，按名称回查编码。
     */
    private List<String> failedUpstreamCodes(Long messageId) {
        List<ZaSysMessageLog> logs = pushLogMapper.selectByMessageId(messageId);
        Map<String, ZaSysMessageLog> latest = new LinkedHashMap<>();
        for (ZaSysMessageLog logItem : logs) {
            String name = channelName(logItem.getTarget());
            latest.putIfAbsent(name, logItem);
        }
        MessageSyncHandler handler = SpringUtils.getBean("gatewayHandler", MessageSyncHandler.class);
        Map<String, String> nameToCode = new HashMap<>();
        for (Map<String, Object> item : handler.upstreamRuntimeStatus()) {
            Object name = item.get("name");
            Object code = item.get("code");
            if (name != null && code != null) {
                nameToCode.put(String.valueOf(name), String.valueOf(code));
            }
        }
        List<String> codes = new ArrayList<>();
        for (Map.Entry<String, ZaSysMessageLog> e : latest.entrySet()) {
            if (!ZaSysMessageLog.STATUS_FAILURE.equals(e.getValue().getStatus())) {
                continue;
            }
            String code = nameToCode.get(e.getKey());
            if (StringUtils.isNotEmpty(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private boolean latestChannelsAllSuccess(Long messageId) {
        List<ZaSysMessageLog> logs = pushLogMapper.selectByMessageId(messageId);
        Map<String, ZaSysMessageLog> latest = new LinkedHashMap<>();
        for (ZaSysMessageLog logItem : logs) {
            latest.putIfAbsent(channelName(logItem.getTarget()), logItem);
        }
        return !latest.isEmpty() && latest.values().stream().allMatch(l -> ZaSysMessageLog.STATUS_SUCCESS.equals(l.getStatus()));
    }

    private static String channelName(String target) {
        if (target == null) {
            return "";
        }
        int idx = target.indexOf('|');
        return (idx > 0 ? target.substring(0, idx) : target).trim();
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
        return mergeTypeCounts(zaSysMessageMapper.countTodayByType());
    }

    @Override
    public List<Map<String, Object>> countTodayByTypeAndPlatform(String pfCode) {
        return mergeTypeCounts(zaSysMessageMapper.countTodayByTypeAndPlatform(pfCode));
    }

    /**
     * 消息的 type 列是逗号拼接的类型列表（如 alarm,alarm,device_upd），这里拆开后按单个类型累加。
     * count(*) 在 MySQL 返回 Long、SQLite 返回 Integer，统一按 Number 取值；type 为空的行归入 unknown。
     */
    private static List<Map<String, Object>> mergeTypeCounts(List<Map<String, Object>> rows) {
        Map<String, Long> merged = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                if (row == null) {
                    continue;
                }
                Object totalObj = row.get("total");
                long total = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0L;
                Object typeObj = row.get("type");
                String types = typeObj == null ? "" : String.valueOf(typeObj);
                boolean matched = false;
                for (String tp : types.split(",")) {
                    String key = tp.trim();
                    if (key.isEmpty()) {
                        continue;
                    }
                    matched = true;
                    merged.merge(key, total, Long::sum);
                }
                if (!matched) {
                    merged.merge("unknown", total, Long::sum);
                }
            }
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<String, Long> entry : merged.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", entry.getKey());
            map.put("total", entry.getValue());
            results.add(map);
        }
        return results;
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
        List<ZaSysMessage> list = zaSysMessageMapper.selectZaSysMessageList(query);
        for (ZaSysMessage msg : list) {
            try {
                MessageViewHelper.fillMessageIdAndSummary(msg);
            } catch (Exception ignored) {
                // 摘要解析失败不影响列表展示
            }
            // 首页只展示摘要，报文正文不下发
            msg.setContent(null);
            msg.setUnifiedContent(null);
        }
        return list;
    }
}
