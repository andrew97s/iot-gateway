package com.zhian.gateway.third.gw;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.domain.ZaSysUpstream;
import com.zhian.gateway.sys.mapper.ZaSysUpstreamMapper;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.gw.message.UnifiedMessage;
import com.zhian.gateway.third.gw.message.UnifiedMessageConverter;
import com.zhian.gateway.third.gw.sender.MessageSender;
import com.zhian.gateway.third.gw.sender.MqMessageSender;
import com.zhian.gateway.third.gw.sender.RedisMessageSender;
import com.zhian.gateway.third.gw.sender.UrlMessageSender;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关核心插件-消息同步处理器，负责与上级平台消息交互
 *
 * 1. 支持多上级平台（za_sys_upstream），每个平台独立配置推送方式：URL / REDIS / MQ
 * 2. 各插件产出的原始事件（MqMessage）在此边界统一转换为标准消息（UnifiedMessage）后推送
 * 3. 每个上级平台的推送结果独立记录（见 UpstreamPushResult），供消息日志/重推使用
 * 4. 兼容旧配置：za_sys_upstream 无数据时回退到 za_sys_platform(code=gateway) 的单通道配置
 */
@Component("gatewayHandler")
@Slf4j
public class MessageSyncHandler implements ThirdHandler {

    public static final String PLATFORM_NAME = "gateway";

    /** 兼容回退通道的虚拟上级代码 */
    public static final String LEGACY_UPSTREAM_CODE = "legacy-gateway";

    private static ZaSysPlatform platform;

    /** upstreamCode -> sender */
    private final Map<String, MessageSender> senders = new ConcurrentHashMap<>();
    /** upstreamCode -> upstream 配置快照 */
    private final Map<String, ZaSysUpstream> upstreams = new ConcurrentHashMap<>();

    private volatile boolean started = false;

    @Autowired
    private UnifiedMessageConverter converter;

    @Override
    public boolean start(ZaSysPlatform platform) {
        MessageSyncHandler.platform = platform;
        reloadUpstreams();
        started = true;
        return true;
    }

    @Override
    public boolean stop() {
        stopAllSenders();
        started = false;
        log.info("消息同步插件已关闭!");
        return true;
    }

    @Override
    public boolean isAlive() {
        if (!started) {
            return false;
        }
        // 无上级配置时视为待机运行；有上级时任一通道存活即为存活
        return senders.isEmpty() || senders.values().stream().anyMatch(MessageSender::isAlive);
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return "gw";
    }

    @Override
    public R control(ControlVo controlVo) {
        return R.error(400, "不支持反控");
    }

    /**
     * 重新加载上级平台配置（上级平台增删改/启停后调用，热生效）
     */
    public synchronized void reloadUpstreams() {
        stopAllSenders();
        List<ZaSysUpstream> list = loadEnabledUpstreams();
        for (ZaSysUpstream upstream : list) {
            try {
                MessageSender sender = createSender(upstream);
                sender.start(upstream.toPlatformConfig());
                senders.put(upstream.getCode(), sender);
                upstreams.put(upstream.getCode(), upstream);
                log.info("上级平台[{}]推送通道已就绪: {}", upstream.getName(), upstream.targetSummary());
            } catch (Exception e) {
                log.error("上级平台[{}]推送通道初始化失败: {}", upstream.getName(), e.getMessage());
                // 初始化失败也登记，推送时记录失败结果，避免消息被静默丢弃
                upstreams.put(upstream.getCode(), upstream);
            }
        }
    }

    private void stopAllSenders() {
        for (Map.Entry<String, MessageSender> e : senders.entrySet()) {
            try {
                e.getValue().stop();
            } catch (Exception ex) {
                log.warn("停止上级[{}]推送通道失败: {}", e.getKey(), ex.getMessage());
            }
        }
        senders.clear();
        upstreams.clear();
    }

    /**
     * 读取启用的上级平台；za_sys_upstream 不可用/为空时回退到旧的 gateway 插件单通道配置
     */
    private List<ZaSysUpstream> loadEnabledUpstreams() {
        try {
            ZaSysUpstreamMapper mapper = SpringUtils.getBean(ZaSysUpstreamMapper.class);
            List<ZaSysUpstream> list = mapper.selectList(Wrappers.lambdaQuery(ZaSysUpstream.class)
                    .eq(ZaSysUpstream::getStatus, ZaSysUpstream.STATUS_ENABLED));
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("读取上级平台配置失败（za_sys_upstream 可能未初始化），回退旧配置: {}", e.getMessage());
        }
        // 兼容旧配置
        List<ZaSysUpstream> fallback = new ArrayList<>();
        if (platform != null && StringUtils.isNotEmpty(platform.getConfig())) {
            ZaSysUpstream legacy = new ZaSysUpstream();
            legacy.setCode(LEGACY_UPSTREAM_CODE);
            legacy.setName("默认推送通道");
            legacy.setStatus(ZaSysUpstream.STATUS_ENABLED);
            String pushType = platform.getConfigStr("pushType", ZaSysUpstream.PUSH_TYPE_MQ);
            legacy.setPushType(pushType);
            legacy.setConfig(platform.getConfig());
            fallback.add(legacy);
        }
        return fallback;
    }

    private MessageSender createSender(ZaSysUpstream upstream) {
        switch (upstream.getPushType() == null ? "" : upstream.getPushType()) {
            case ZaSysUpstream.PUSH_TYPE_URL:
                return new UrlMessageSender(upstream.toPlatformConfig());
            case ZaSysUpstream.PUSH_TYPE_MQ:
                return new MqMessageSender();
            case ZaSysUpstream.PUSH_TYPE_REDIS:
            default:
                return new RedisMessageSender();
        }
    }

    // ==================== 推送 ====================

    /**
     * 兼容入口：插件推送原始事件（MqMessage）或字符串。
     * MqMessage 将先转换为统一消息再推送。
     */
    @Override
    public void processMsg(Object msgObj) {
        if (!started) {
            log.error("{}消息同步插件暂时停止,不处理消息：{}", getPlatform(), msgObj);
            return;
        }
        String unifiedJson;
        if (msgObj instanceof MqMessage) {
            unifiedJson = converter.convert((MqMessage) msgObj).toJson();
        } else if (msgObj instanceof UnifiedMessage) {
            unifiedJson = ((UnifiedMessage) msgObj).toJson();
        } else if (msgObj instanceof String) {
            unifiedJson = (String) msgObj;
        } else {
            unifiedJson = JSONObject.toJSONString(msgObj);
        }
        List<UpstreamPushResult> results = pushToUpstreams(unifiedJson);
        for (UpstreamPushResult r : results) {
            if (!r.isSuccess()) {
                throw new IllegalStateException("上级[" + r.getUpstreamName() + "]推送失败: " + r.getError());
            }
        }
    }

    /**
     * 将统一消息（JSON）推送至全部启用的上级平台，逐一记录结果。
     *
     * @param unifiedJson 统一消息 JSON（含 messageId，上级按其去重实现幂等）
     * @return 每个上级平台的推送结果
     */
    public List<UpstreamPushResult> pushToUpstreams(String unifiedJson) {
        List<UpstreamPushResult> results = new ArrayList<>();
        if (upstreams.isEmpty()) {
            return results;
        }
        for (Map.Entry<String, ZaSysUpstream> e : upstreams.entrySet()) {
            ZaSysUpstream upstream = e.getValue();
            UpstreamPushResult result = new UpstreamPushResult();
            result.setUpstreamCode(upstream.getCode());
            result.setUpstreamName(upstream.getName());
            result.setPushType(upstream.getPushType());
            result.setTarget(upstream.targetSummary());
            MessageSender sender = senders.get(e.getKey());
            try {
                if (sender == null) {
                    throw new IllegalStateException("推送通道未初始化");
                }
                sender.send(unifiedJson);
                result.setSuccess(true);
            } catch (Exception ex) {
                result.setSuccess(false);
                result.setError(ex.getMessage());
                log.error("上级平台[{}]推送失败: {}", upstream.getName(), ex.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 转换 MqMessage 为统一消息（供插件基类在推送前取得统一报文以记录日志）
     */
    public UnifiedMessage convert(MqMessage mq) {
        return converter.convert(mq);
    }

    /**
     * 测试指定上级平台连通性：临时创建通道并探活
     */
    public UpstreamPushResult testUpstream(ZaSysUpstream upstream) {
        UpstreamPushResult result = new UpstreamPushResult();
        result.setUpstreamCode(upstream.getCode());
        result.setUpstreamName(upstream.getName());
        result.setPushType(upstream.getPushType());
        result.setTarget(upstream.targetSummary());
        MessageSender sender = null;
        try {
            sender = createSender(upstream);
            sender.start(upstream.toPlatformConfig());
            result.setSuccess(sender.isAlive());
            if (!result.isSuccess()) {
                result.setError("通道创建成功但探活失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        } finally {
            if (sender != null) {
                try {
                    sender.stop();
                } catch (Exception ignore) {
                }
            }
        }
        return result;
    }

    /** 各上级平台运行时状态（供管理页展示） */
    public List<Map<String, Object>> upstreamRuntimeStatus() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, ZaSysUpstream> e : upstreams.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            ZaSysUpstream upstream = e.getValue();
            MessageSender sender = senders.get(e.getKey());
            item.put("code", upstream.getCode());
            item.put("name", upstream.getName());
            item.put("pushType", upstream.getPushType());
            item.put("target", upstream.targetSummary());
            item.put("alive", sender != null && sender.isAlive());
            list.add(item);
        }
        return list;
    }

    @Override
    public String getDescription() {
        return "消息同步模块 - 统一消息格式化后推送至上级平台（HTTP / MQ / Redis，支持多平台）";
    }

    @Override
    public String getConnectionInfo() {
        if (!started) {
            return "已停止";
        }
        if (upstreams.isEmpty()) {
            return "未配置上级平台";
        }
        long alive = senders.values().stream().filter(MessageSender::isAlive).count();
        return String.format("上级平台 %d/%d 在线", alive, upstreams.size());
    }

    @Override
    public java.util.List<Map<String, Object>> getConfigSchema() {
        // 上级平台连接改由「系统配置-上级平台」维护（za_sys_upstream），此处无插件级参数
        return new ArrayList<>();
    }

    /**
     * 当前推送目标摘要（兼容旧调用）
     */
    public Optional<PushTargetSnapshot> currentPushTarget() {
        if (upstreams.isEmpty()) {
            return Optional.of(new PushTargetSnapshot("unknown", "未配置上级平台"));
        }
        StringBuilder sb = new StringBuilder();
        String mode = "multi";
        for (ZaSysUpstream u : upstreams.values()) {
            if (sb.length() > 0) {
                sb.append(" ; ");
            }
            sb.append(u.getName()).append('(').append(u.targetSummary()).append(')');
            mode = u.getPushType();
        }
        if (upstreams.size() > 1) {
            mode = "multi";
        }
        return Optional.of(new PushTargetSnapshot(mode, sb.toString()));
    }

    /**
     * 单个上级平台的推送结果
     */
    @Data
    public static class UpstreamPushResult {
        private String upstreamCode;
        private String upstreamName;
        /** url / redis / mq */
        private String pushType;
        private String target;
        private boolean success;
        private String error;
    }

    /**
     * 推送目标快照（兼容旧调用）
     */
    public static class PushTargetSnapshot {
        public final String pushMode;
        public final String targetAddress;

        public PushTargetSnapshot(String pushMode, String targetAddress) {
            this.pushMode = pushMode;
            this.targetAddress = targetAddress;
        }
    }
}
