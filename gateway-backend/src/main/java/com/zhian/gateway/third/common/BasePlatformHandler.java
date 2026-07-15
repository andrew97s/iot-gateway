package com.zhian.gateway.third.common;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.cascade.CascadeHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
     * 设备在线状态Map，key=设备ID , value=设备最近一次通讯时间
     */
    public ConcurrentHashMap<Long, Long> statusMap = new ConcurrentHashMap<>();

    public volatile boolean running = false;

    protected volatile ZaSysPlatform platform;

    @Override
    public boolean isAlive() {
        return running;
    }

    @Override
    public boolean start(ZaSysPlatform platform) {
        this.platform = platform;
        running = true;
        return running;
    }

    @Override
    public boolean stop() {
        this.platform = null;
        running = false;
        return running;
    }

    /**
     * 检查设备状态, 子类通过{@link DeviceUtil.syncDevice( DeviceUpdReq )}方法同步设备在线状态&通讯时间
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
     */
    public DeviceSyncInfo syncDeviceStatus() {
        return DeviceSyncInfo.success(0);
    }

    /**
     * 推送消息
     *
     * @param message the message
     */
    public void consumeMsg(MqMessage message) {
        //推送到RabbitMQ
        messageSyncHandler.processMsg(message);
        //推送到上级
        cascadeHandler.pushMsg(message);
    }

    /**
     * Do control ajax result.
     *
     * @param controlVo the control vo
     * @return the ajax result
     */
    protected AjaxResult doControl(ControlVo controlVo) {
        return AjaxResult.error("当前暂不支持反控操作!");
    }

    @Override
    public AjaxResult control(ControlVo controlVo) {
        AjaxResult result = null;
        try {
            result = doControl(controlVo);
        } catch (Exception e) {
            log.error("反控失败:{}", e.getMessage());
            result = AjaxResult.error(e.getMessage());
        } finally {
            ZaSysDevice device = MessageUtil.getDevice();
            if (device == null) {
                device = deviceService.selectZaSysDeviceById(controlVo.getDeviceId());
            }
            if (device == null) {
                device = new ZaSysDevice();
                device.setPfCode(getPlatform());
            }
            ProcessInfo info = ProcessInfo.newControl(device, controlVo, result);
            logMessage(info);
            MessageUtil.clear();
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
        // 记录操作日志
        ProcessInfo info = null;
        try {
            info = doProcessMsg((T) msgObject);
            // 执行推送上级平台
            if (info != null && CollUtil.isNotEmpty(info.getMsgList())) {
                List<MqMessage> msgList = info.getMsgList();
                for (MqMessage message : msgList) {
                    consumeMsg(message);
                }
            }
        } catch (Exception e) {
            log.error("处理消息发生异常:{}", e.getMessage());
            ZaSysDevice device = MessageUtil.getDevice();
            if (device == null) {
                device = new ZaSysDevice();
                device.setPfCode(device.getPfCode());
            }
            String msg = msgObject instanceof String ? (String) msgObject : JSONObject.toJSONString(msgObject);
            info = ProcessInfo.newError(device, msg, e.getMessage());
        } finally {
            // 记录日志
            logMessage(info);
            // 清除设备信息
            MessageUtil.clear();
        }
    }

    /**
     * Log message.
     *
     * @param info the info
     */
    protected void logMessage(ProcessInfo info) {
        if (info == null) {
            return;
        }

        // 判断当前插件最大支持记录消息的数量
        zaSysMessageService.log(info);
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