package com.zhian.gateway.third.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.core.message.AlarmPayload;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MessageDevice;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.PluginHealthResult;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.cascade.CascadeHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import com.zhian.gateway.third.jadebird.vo.Facility;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.N;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认的对接处理器基类
 * 1. 提供基础日志记录能力
 * 2. 提供设备状态同步方法
 * 日志的记录包括消息处置参数、状态、结果、推送状态、推送结果
 *
 * @param <T> the type parameter
 */
@Slf4j
@Component
public abstract class BasePlatformHandler<T> implements ThirdHandler {
    /**
     * The Cache.
     */
    @Autowired
    protected Cache cache;
    /**
     * The Gateway handler.
     */
    @Autowired
    protected MessageSyncHandler messageSyncHandler;
    /**
     * The Cascade handler.
     */
    @Autowired
    protected CascadeHandler cascadeHandler;
    /**
     * The Za sys device service.
     */
    @Autowired
    protected IZaSysDeviceService deviceService;
    /**
     * The Za sys error service.
     */
    @Autowired
    protected IZaSysErrorService zaSysErrorService;
    /**
     * The Za sys message service.
     */
    @Autowired
    protected IZaSysMessageService zaSysMessageService;
    /**
     * The Type mapping service.
     */
    @Autowired
    protected TypeMappingService typeMappingService;
    /**
     * 设备在线状态Map，key=设备ID , value=设备最近一次通讯时间
     */
    public ConcurrentHashMap<Long, Long> statusMap = new ConcurrentHashMap<>();

    /**
     * The Running.
     */
    public volatile boolean running = false;

    /**
     * The Platform.
     */
    protected volatile ZaSysPlatform platform;
    /**
     * 最近一次收到厂商消息的时间，仅用于健康状态说明。
     */
    protected volatile long lastActivityTime;

    @Override
    public boolean isAlive() {
        return running && platform != null;
    }

    @Override
    public PluginHealthResult checkHealth() {
        if (!isAlive()) {
            return PluginHealthResult.unhealthy("插件运行资源未就绪");
        }
        return PluginHealthResult.healthy(getConnectionInfo());
    }

    @Override
    public String getConnectionInfo() {
        if (!isAlive()) {
            return "插件已停止";
        }
        return lastActivityTime > 0
                ? "接收端正常，最近通信时间 " + new Date(lastActivityTime)
                : "接收端已就绪，等待厂商消息";
    }

    @Override
    public boolean start(ZaSysPlatform platform) {
        this.platform = platform;
        running = true;
        return running;
    }

    @Override
    public boolean stop() {
        log.info("停止插件:{}", getPlatform());
        this.platform = null;
        running = false;
        return true;
    }

    @Override
    public void syncDevice() {

    }


    /**
     * 检查设备状态, 子类通过{@link DeviceUtil#syncDevice(SyncDevice)}方法同步设备在线状态&通讯时间
     * 1. 设备离线触发场景
     * 1.1 设备本身触发离线告警
     * 1.2 设备心跳超时
     * 2. 设备在线触发场景
     * 1.1 设备本身回调在线消息事件
     * 1.2 设备心跳消息
     */
    protected void checkDeviceStatus() {

    }

    /**
     * 同步所有设备状态
     *
     * @return the device sync info
     */
    public DeviceSyncInfo syncDeviceStatus() {
        return DeviceSyncInfo.success(0);
    }

    /**
     * 推送消息（兼容旧调用：转换统一消息后推送全部上级平台 + 级联）
     *
     * @param message the message
     */
    public void consumeMsg(MqMessage message) {
        //转换统一消息并推送到全部上级平台
        messageSyncHandler.processMsg(message);
        //推送到级联上级
        cascadeHandler.pushMsg(message);
    }

    /**
     * 转换统一消息并推送全部上级平台，返回统一报文与各平台推送结果（写入消息日志）
     *
     * @param info the info
     */
    protected void convertAndPush(ProcessInfo info) {
        List<Message> msgList = info.getMsgList();

        // 统一消息
        String unifiedJson = JSON.toJSONString(msgList.size() == 1 ? msgList.get(0) : msgList);
        // 执行推送
        List<MessageSyncHandler.UpstreamPushResult> results = new ArrayList<>(
                messageSyncHandler.pushToUpstreams(unifiedJson)
        );
        // TODO 3. 级联上级平台（WebSocket）沿用原始事件通道
        try {
            // cascadeHandler.pushMsg(message);
        } catch (Exception e) {
            log.warn("级联推送失败: {}", e.getMessage());
        }

        // 保存推送结果
        info.setUnifiedContent(unifiedJson);
        info.setPushResults(results);
    }

    /**
     * Do control ajax result.
     *
     * @param controlVo the control vo
     * @return the ajax result
     */
    protected R doControl(ControlVo controlVo) {
        return R.error("当前暂不支持反控操作!");
    }

    @Override
    public R control(ControlVo controlVo) {
        R result = null;
        try {
            MsgProcessContext.start(controlVo, platform);
            result = doControl(controlVo);
        } catch (Exception e) {
            log.error("反控失败:{}", e.getMessage());
            result = R.error(e.getMessage());
        } finally {
            MsgProcessContext.getProcessInfo().setDevice(controlVo.getDevice());

            MsgProcessContext.addMsg(MessageBuilder.buildControl(controlVo.getDevice(), controlVo));
            // 处理结果
            MsgProcessContext.getProcessInfo().setUnifiedContent(JSON.toJSONString(result));
            // 控制参数
            MsgProcessContext.getProcessInfo().setContent(JSON.toJSONString(controlVo));
            logMessage(MsgProcessContext.finishAndGet());
        }

        return result;
    }

    /**
     * 提供子类实现msgObj自动类型转换
     *
     * @param msgObj the msg obj
     * @return the process info
     */
    protected ProcessInfo doProcessMsg(T msgObj) {
        return null;
    }

    /**
     * 默认调用{@link #doProcessMsg(Object)}实现msgObject自动类型转换
     *
     * @param msgObject msgObject
     */
    @Override
    @SuppressWarnings("unchecked")
    public void processMsg(Object msgObject) {
        if (Objects.isNull(msgObject)) {
            return;
        }
        lastActivityTime = System.currentTimeMillis();
        // 记录操作日志
        ProcessInfo info = null;
        try {
            MsgProcessContext.start(msgObject, platform);
            if (!isAlive()) {
                String msg = String.format("处理:%s消息失败,插件已停止!", getPlatform());
                MsgProcessContext.failed(msg);
                return;
            }
            doProcessMsg((T) msgObject);
            info = MsgProcessContext.getProcessInfo();
            // 转换统一消息并推送上级平台，收集统一报文与逐平台推送结果
            if (info != null && CollUtil.isNotEmpty(info.getMsgList())) {
                // 处理后置业务
                postProcess(info);
                // 消息转换&推送
                convertAndPush(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("处理消息发生异常:{}", e.getMessage());
            MsgProcessContext.failed(e.getMessage());
        } finally {
            // 清除设备信息
            MessageUtil.clear();
            // 记录日志（原始报文 + 统一消息 + 各上级平台同步状态）
            logMessage(MsgProcessContext.finishAndGet());
        }
    }

    /**
     * Log message.
     *
     * @param info the info
     */
    protected void logMessage(ProcessInfo info) {
        if (info == null || CollUtil.isEmpty(info.getMsgList())) {
            return;
        }

        // 判断当前插件最大支持记录消息的数量
        zaSysMessageService.log(info);
    }

    /**
     * 手动同步设备
     *
     * @param fetchDevice 提取设备逻辑
     * @param rawDevice   设备原始消息
     */
    protected void manualSyncDevice(Callable<ZaSysDevice> fetchDevice, String rawDevice) {
        try {
            MsgProcessContext.start(rawDevice, platform);

            ZaSysDevice device = fetchDevice.call();

            MsgProcessContext.getProcessInfo().setDevice(device);

            convertAndPush(MsgProcessContext.getProcessInfo());
        } catch (Exception e) {
            log.error("手动同步设备失败:{}", e.getMessage());
            MsgProcessContext.failed(e.getMessage());
        } finally {
            logMessage(MsgProcessContext.finishAndGet());
        }
    }

    private void postProcess(ProcessInfo info) {
        List<Message> msgList = info.getMsgList();
        msgList = msgList.stream().filter(Objects::nonNull).collect(Collectors.toList());

        if (CollUtil.isNotEmpty(msgList)) {
            // 设备设备信息
            if (info.getDevice() == null) {
                info.setDevice(deviceService.selectZaSysDeviceById(msgList.get(0).getDevice().getDeviceId()));
            }

            msgList.forEach(msg -> {
                if (msg.getPayload() instanceof AlarmPayload) {
                    AlarmPayload payload = (AlarmPayload) msg.getPayload();
                    ZaSysDevice device = info.getDevice();
                    // 设备离线
                    if (StrUtil.equals(payload.getCode(), "55") && StrUtil.equals(device.getOnline(), "1")) {
                        device.setOnline("0");
                        deviceService.updateZaSysDevice(device);
                        log.info("设备离线告警-同步设备状态为离线!");
                    }

                    // 设备在线
                    if (StrUtil.equals(payload.getCode(), "54") && StrUtil.equals(device.getOnline(), "0")) {
                        device.setOnline("1");
                        deviceService.updateZaSysDevice(device);
                        log.info("设备在线告警-同步设备状态为在线!");
                    }
                }
            });
        }
    }

    public ZaMonitorType fetchMonitorType(String code) {
        TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaMonitorType> type = typeMapping.resolveMonitorType(getPlatform(), code);
        if (!type.isPresent()) {
            throw new IllegalArgumentException("获取(" + getPlatform() + ")监测值类型失败:" + code + "未注册!");
        }

        return type.get();
    }

    /**
     * 推送告警消息
     *
     * @param device    平台设备信息
     * @param eventType 告警事件类型(源告警类型)
     * @param msg       源告警消息体
     * @return the mq message
     */
    public MqMessage genAlarm(ZaSysDevice device, String eventType, String msg) {
        return genAlarm(device, eventType, null, msg);
    }

    /**
     * Push alarm mq message.
     *
     * @param device    the device
     * @param eventType the event type
     * @param imageUrl  the image url
     * @param msg       the msg
     * @return the mq message
     */
    public MqMessage genAlarm(ZaSysDevice device, String eventType, String imageUrl, String msg) {
        // 提取基础设备信息
        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setType(device.getType());
        facility.setCode(device.getCode());
        facility.setName(device.getName());
        facility.setModel(device.getModel());
        facility.setNet(device.getNet());
        facility.setWireless(Constants.YES.equals(device.getWireless()));
        facility.setOnLine(Constants.YES.equals(device.getOnline()));

        // 推送告警
        return MqMessage.createAlarm(
                device.getId(),
                getProtocol(),
                facility,
                eventType,
                msg,
                imageUrl
        );
    }
}