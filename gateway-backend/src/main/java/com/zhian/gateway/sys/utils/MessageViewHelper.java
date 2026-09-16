package com.zhian.gateway.sys.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.consts.MessageConstants;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysMessageLog;

import java.util.List;

/**
 * 消息列表/详情展示字段填充（messageId / 摘要 / 同步徽标）
 */
public final class MessageViewHelper {
    private MessageViewHelper() {
    }

    public static void enrich(ZaSysMessage msg, int upstreamTotal, List<ZaSysMessageLog> latestLogs) {
        if (msg == null) {
            return;
        }
        fillMessageIdAndSummary(msg);
        fillSyncBadge(msg, upstreamTotal, latestLogs);
    }

    public static void fillMessageIdAndSummary(ZaSysMessage msg) {
        String unified = msg.getUnifiedContent();
        String content = msg.getContent();
        JSONObject obj = firstObject(unified);
        if (obj == null || StrUtil.equals(msg.getType() , "control")) {
            obj = firstObject(content);
        }
        if (obj != null) {
            String mid = obj.getString("messageId");
            if (StringUtils.isEmpty(mid)) {
                mid = obj.getString("uuid");
            }
            if (StringUtils.isEmpty(mid)) {
                mid = obj.getString("id");
            }
            msg.setMessageId(mid);
            msg.setSummary(buildSummary(msg.getType(), obj));
        }
        if (StringUtils.isEmpty(msg.getMessageId()) && msg.getId() != null) {
            msg.setMessageId(String.valueOf(msg.getId()));
        }
        if (StringUtils.isEmpty(msg.getSummary())) {
            msg.setSummary(defaultSummary(msg.getType()));
        }
    }

    private static void fillSyncBadge(ZaSysMessage msg, int upstreamTotal, List<ZaSysMessageLog> latestLogs) {
        if ("control".equalsIgnoreCase(msg.getType())) {
            msg.setSyncTotal(0);

            int syncSuccess = 0;
            String content = msg.getUnifiedContent();
            if (StrUtil.isNotBlank(content)) {
                R result = JSON.parseObject(content, R.class);
                syncSuccess = result.isSuccess() ? 1 : 0;
            }
            msg.setSyncSuccess(syncSuccess);
            msg.setSyncLabel(syncSuccess == 1 ? "反控成功" : "反控失败");
            return;
        }
        int total = Math.max(upstreamTotal, 0);
        if (latestLogs != null && !latestLogs.isEmpty()) {
            long ok = latestLogs.stream().filter(l -> ZaSysMessageLog.STATUS_SUCCESS.equals(l.getStatus())).count();
            int t = latestLogs.size();
            msg.setSyncSuccess((int) ok);
            msg.setSyncTotal(t);
            if (ok == t) {
                msg.setSyncLabel(ok + "/" + t + " 成功");
            } else if (ok == 0) {
                msg.setSyncLabel("0/" + t + " 失败");
            } else {
                msg.setSyncLabel(ok + "/" + t + " · 部分失败");
            }
            return;
        }
        msg.setSyncTotal(total);
        if (ZaSysMessage.SEND_STATUS_SENT.equals(msg.getSendStatus())) {
            msg.setSyncSuccess(total);
            msg.setSyncLabel(total > 0 ? total + "/" + total + " 成功" : "已推送");
        } else if (ZaSysMessage.SEND_STATUS_FAILED.equals(msg.getSendStatus())) {
            msg.setSyncSuccess(0);
            String retry = msg.getRetryCount() != null && msg.getRetryCount() > 0
                    ? " · 重试中(" + msg.getRetryCount() + ")" : "";
            msg.setSyncLabel(total > 0 ? "0/" + total + " 失败" + retry : "推送失败");
        } else {
            msg.setSyncSuccess(0);
            msg.setSyncLabel(total > 0 ? "待同步" : "未推送");
        }
    }

    private static String buildSummary(String type, JSONObject obj) {
        JSONObject payload = obj.getJSONObject("payload");
        if (payload == null) {
            payload = obj;
        }
        if (StrUtil.contains(type, MessageConstants.MSG_TYPE_ALARM)) {
            String desc = firstNonEmpty(
                    payload.getString("desc"),
                    payload.getString("name")
            );
            Object level = payload.get("level");
            if (StringUtils.isNotEmpty(desc) && level != null) {
                return desc + " · 级别 " + level;
            }
            return StringUtils.isNotEmpty(desc) ? desc : "告警事件";
        }
        if (StrUtil.contains(type, MessageConstants.MSG_TYPE_TELEMETRY)) {
            JSONObject metrics = payload.getJSONObject("metrics");
            if (metrics != null && !metrics.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (String key : metrics.keySet()) {
                    if (i++ > 0) {
                        sb.append(" / ");
                    }
                    sb.append(key).append(' ').append(metrics.get(key));
                    if (i >= 3) {
                        break;
                    }
                }
                return sb.toString();
            }
            return firstNonEmpty(payload.getString("description"), "监测数据");
        }
        if (StrUtil.contains(type, "device")) {
            String event = firstNonEmpty(
                    payload.getString("event"),
                    payload.getString("eventType"),
                    obj.getString("eventType")
            );
            return StringUtils.isNotEmpty(event) ? event : "设备事件";
        }
        if (StrUtil.startWith(type, MessageConstants.MSG_TYPE_CONTROL)) {
            String cmd = firstNonEmpty(payload.getString("command"), payload.getString("action"));
            return StringUtils.isNotEmpty(cmd) ? cmd : "反控指令";
        }
        return firstNonEmpty(
                payload.getString("description"),
                payload.getString("eventDescription"),
                defaultSummary(type)
        );
    }

    private static String defaultSummary(String type) {
        if ("alarm".equalsIgnoreCase(type)) {
            return "告警事件";
        }
        if ("business".equalsIgnoreCase(type) || "monitor".equalsIgnoreCase(type)) {
            return "监测数据";
        }
        if ("device".equalsIgnoreCase(type)) {
            return "设备事件";
        }
        if ("control".equalsIgnoreCase(type)) {
            return "反控指令";
        }
        return type != null ? type : "消息";
    }

    private static JSONObject firstObject(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        try {
            Object parsed = JSON.parse(text.trim());
            if (parsed instanceof JSONObject) {
                return (JSONObject) parsed;
            }
            if (parsed instanceof JSONArray) {
                JSONArray arr = (JSONArray) parsed;
                if (!arr.isEmpty() && arr.get(0) instanceof JSONObject) {
                    return arr.getJSONObject(0);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String firstNonEmpty(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (StringUtils.isNotEmpty(v)) {
                return v;
            }
        }
        return null;
    }
}
