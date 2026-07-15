package com.zhian.gateway.third.gw;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.ThirdHandler;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.third.gw.sender.MessageSender;
import com.zhian.gateway.third.gw.sender.MqMessageSender;
import com.zhian.gateway.third.gw.sender.RedisMessageSender;
import com.zhian.gateway.third.gw.sender.UrlMessageSender;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.zhian.gateway.third.gw.consts.GatewayConstants.PUSH_TYPE_MQ;
import static com.zhian.gateway.third.gw.consts.GatewayConstants.PUSH_TYPE_URL;
import static com.zhian.gateway.third.gw.consts.GatewayConstants.PUSH_TYPE_REDIS;

/**
 * 网关核心插件-消息同步处理器，负责与上级平台消息交互逻辑
 * 1、默认实现三类推送方式： URL 、REDIS 、MQ
 * 2、反控时，通过网关的设备库找到设备对接平台，下发指令
 */
@Component("gatewayHandler")
@Slf4j
public class MessageSyncHandler implements ThirdHandler {

    public static final String PLATFORM_NAME = "gateway";

    private static ZaSysPlatform platform;

    private MessageSender sender;

    @Override
    public boolean start(ZaSysPlatform platform) {
        if (isAlive()) {
            log.info("{} 状态为运行中, 放弃执行start ！", platform.getName());
            return true;
        }
        MessageSyncHandler.platform = platform;
        if (StringUtils.isEmpty(platform.getConfig())) {
            log.info("未配置RabbitMQ，将等待青鸟云平台推送过来的数据");
            return false;
        }

        //先初始化
        initMessageSender();
        //再启动
        sender.start(platform);

        return true;
    }

    @Override
    public boolean stop() {
        boolean stop = true;
        if (sender != null) {
            stop = sender.stop();
        }
        sender = null;
        log.info("{} 已关闭!", platform.getName());
        return stop;
    }

    @Override
    public boolean isAlive() {
        return sender != null && sender.isAlive();
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return "gw";
    }

    /**
     * 转发控制指令，先从设备库找到设备，然后从网关的设备库里找到对接的平台，下发对控制指令
     *
     * @param controlVo controlVo
     * @return the result
     */
    @Override
    public AjaxResult control(ControlVo controlVo) {
        return AjaxResult.error(400, "不支持反控");
    }

    @Override
    public void processMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("{}网关插件暂时停止,不处理消息：{}", getPlatform(), msgObj);
            return;
        }
        String msgStr = null;
        if (msgObj instanceof String) {
            msgStr = msgObj.toString();
        } else {
            msgStr = JSONObject.toJSONString(msgObj);
        }
        sender.send(msgStr);
    }

    @Override
    public String getDescription() {
        return "消息推送模块 - 将网关接收的消息推送至下游系统（MQ / HTTP / Redis）";
    }

    @Override
    public String getConnectionInfo() {
        if (sender == null) return "未配置";
        return sender.isAlive() ? "推送中" : "已断开";
    }

    @Override
    public java.util.List<Map<String, Object>> getConfigSchema() {
        java.util.List<Map<String, Object>> schema = new ArrayList<>();

        Map<String, Object> pushType = new LinkedHashMap<>();
        pushType.put("code",         "pushType");
        pushType.put("name",         "推送方式");
        pushType.put("desc",         "选择消息下发到下游的方式；修改后保存将触发插件重载。");
        pushType.put("type",         "select");
        pushType.put("required",     true);
        pushType.put("defaultValue", PUSH_TYPE_MQ);
        pushType.put("options", java.util.Arrays.asList(
                optionOf("MQ消息队列", PUSH_TYPE_MQ),
                optionOf("HTTP URL推送", PUSH_TYPE_URL),
                optionOf("Redis队列", PUSH_TYPE_REDIS)
        ));
        schema.add(pushType);

        Map<String, Object> pushUrls = new LinkedHashMap<>();
        pushUrls.put("code",         "pushUrls");
        pushUrls.put("name",         "推送地址列表");
        pushUrls.put("desc",         "pushType 为 url 时填写；每行一个地址或使用英文逗号分隔。");
        pushUrls.put("type",         "textarea");
        pushUrls.put("required",     false);
        pushUrls.put("placeholder",  "每行一个URL，或逗号分隔");
        pushUrls.put("rows",         4);
        schema.add(pushUrls);

        Map<String, Object> mqQueue = new LinkedHashMap<>();
        mqQueue.put("code",         "queueName");
        mqQueue.put("name",         "MQ队列名");
        mqQueue.put("desc",         "RabbitMQ 队列名称；留空时由网关使用内置默认队列。");
        mqQueue.put("type",         "string");
        mqQueue.put("required",      false);
        mqQueue.put("defaultValue", "za_monitor");
        mqQueue.put("placeholder",  "默认 za_monitor；pushType=mq 时生效");
        schema.add(mqQueue);

        return schema;
    }

    private static Map<String, Object> optionOf(String label, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        return m;
    }

    /**
     * 初始化消息发送器.
     */
    public void initMessageSender() {
        String pushType = platform.getConfigStr("pushType", PUSH_TYPE_MQ);

        switch (pushType) {
            // URL 实现的消息推送
            case PUSH_TYPE_URL: {
                // 校验URL是否存在
                String pushUrl = platform.getConfigStr("pushUrl");
                Assert.notBlank(pushUrl, "网关初始化失败, 当推送方式为url时,pushUrl不能为空!");
                // 校验URL是否可达
                // boolean urlReachable = HttpUtils.isUrlReachable(pushUrl);
                // if (!urlReachable) {
                //     throw new IllegalArgumentException("网关初始化失败,pushUrl(" + pushUrl + ")不可达!");
                // }
                sender = new UrlMessageSender(platform);
                break;
            }
            // MQ实现的消息推送
            case PUSH_TYPE_MQ: {
                sender = new MqMessageSender();
                break;
            }
            // 默认为REDIS实现的缓存队列
            default: sender = new RedisMessageSender();
        }
    }

    /**
     * 当前网关向下游推送的目标摘要（用于消息推送记录展示）。
     */
    public Optional<PushTargetSnapshot> currentPushTarget() {
        if (platform == null) {
            return Optional.of(new PushTargetSnapshot("unknown", "网关未配置或未启动"));
        }
        String raw = platform.getConfigStr("pushType", PUSH_TYPE_MQ);
        String modeKey = normalizePushModeKey(raw);
        return Optional.of(new PushTargetSnapshot(modeKey, buildTargetAddressSummary(modeKey)));
    }

    private static String normalizePushModeKey(String raw) {
        if (raw == null) {
            return PUSH_TYPE_REDIS;
        }
        String r = raw.toLowerCase(Locale.ROOT).trim();
        if (PUSH_TYPE_MQ.equals(r)) {
            return PUSH_TYPE_MQ;
        }
        if (PUSH_TYPE_URL.equals(r)) {
            return PUSH_TYPE_URL;
        }
        if (PUSH_TYPE_REDIS.equals(r)) {
            return PUSH_TYPE_REDIS;
        }
        return r.isEmpty() ? PUSH_TYPE_REDIS : r;
    }

    private String buildTargetAddressSummary(String modeKey) {
        switch (modeKey) {
            case PUSH_TYPE_MQ:
                return buildMqTargetSummary();
            case PUSH_TYPE_URL:
                return buildUrlTargetSummary();
            case PUSH_TYPE_REDIS:
                return buildRedisTargetSummary();
            default:
                return "";
        }
    }

    private String buildMqTargetSummary() {
        String ip = platform.getConfigStr("ip", "");
        int port = platform.getConfigInt("port");
        String vhost = platform.getConfigStr("vhost", "/");
        return String.format(
                "RabbitMQ %s:%d vhost=%s exchange=%s routingKey=%s queue=%s",
                ip, port, vhost,
                MqMessageSender.QUEUE_EXCHANGE,
                MqMessageSender.QUEUE_KEY,
                MqMessageSender.QUEUE_NAME);
    }

    private String buildUrlTargetSummary() {
        String pushUrls = platform.getConfigStr("pushUrls");
        if (StringUtils.isNotEmpty(pushUrls)) {
            String pu = pushUrls.trim();
            if (pu.startsWith("[")) {
                try {
                    List<String> arr = JSON.parseArray(pu, String.class);
                    return String.join(" ; ", arr);
                } catch (Exception ignore) {
                    // fall through
                }
            }
            return pu.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        }
        String pushUrl = platform.getConfigStr("pushUrl");
        return StringUtils.isNotEmpty(pushUrl) ? pushUrl.trim() : "(未配置推送URL)";
    }

    private String buildRedisTargetSummary() {
        String host = platform.getConfigStr("ip", "127.0.0.1");
        String port = platform.getConfigStr("port", "6379");
        String db = platform.getConfigStr("db", "6");
        return String.format("Redis %s:%s db=%s list=%s", host, port, db, "gateway_queue");
    }

    /**
     * 推送目标快照（写入推送记录表）
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
