package com.zhian.gateway.third.others.mk;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.others.mk.constants.MkConsts;
import com.zhian.gateway.third.others.mk.protocol.MkServer;
import com.zhian.gateway.third.others.mk.vo.MkCanMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.zhian.gateway.third.others.mk.constants.MkConsts.DEVICE_MODEL_S905R;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.DEVICE_TYPE_CODE;

/**
 * 铭控业务处理器
 *
 * @author tongwenjin
 * @since 2024-12-4
 */

@Component
@Slf4j
public class MkHandler extends BasePlatformHandler<MkCanMsg> {

    public static String CODE = "mk";

    private HashMap<String, Long> alarmTimeMap = new HashMap<>();

    @Override
    public boolean start(ZaSysPlatform platform) {
        super.start(platform);
        try {
            // 启动MkServer
            MkServer.start(this);
            // 异步检查设备状态
            CompletableFuture.runAsync(this::checkDeviceStatus);
        } catch (Exception e) {
            log.error("铭控server启动失败,msg:{}", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        // 查询当前平台在线的设备
        List<ZaSysDevice> deviceList = deviceService.list(
                Wrappers.lambdaUpdate(ZaSysDevice.class)
                        .eq(ZaSysDevice::getPfCode, getPlatform())
                        .eq(ZaSysDevice::getOnline, Constants.YES)
                        .isNull(ZaSysDevice::getCascadeId)
        );
        for (ZaSysDevice device : deviceList) {
            // 上次心跳时间
            long lastHeartTime = statusMap.getOrDefault(device.getId(), System.currentTimeMillis());
            // 超过60min没有心跳 - 将对应设备状态改为离线状态
            if (DateUtil.between(new Date(), new Date(lastHeartTime), DateUnit.MINUTE) > 60) {
                // 修改设备状态
                device.setOnline(Constants.NO);
                deviceService.updateZaSysDevice(device);
                // 推送离线告警
                DeviceUtil.pushDevice(DeviceUpdReq.newOfflineReq(device, "心跳超时"));
            }
        }

        return DeviceSyncInfo.success(deviceList.size());
    }

    @Override
    public boolean stop() {
        super.stop();
        MkServer.stop();
        return true;
    }

    @Override
    public boolean isAlive() {
        return running && MkServer.isActive();
    }

    @Override
    public String getPlatform() {
        return CODE;
    }

    @Override
    public String getProtocol() {
        return CODE;
    }

    @Override
    public AjaxResult doControl(ControlVo controlVo) {
        return AjaxResult.error("操作失败,铭控暂不支持反控操作!");
    }

    @Override
    public ProcessInfo doProcessMsg(MkCanMsg msg) {
        // 此方法由MkServer调用
        String type = msg.getType();
        ProcessInfo processInfo = null;
        switch (type) {
            // 告警消息
            case MkConsts.MSG_TYPE_ALARM: {
                // 同步设备
                ZaSysDevice device = syncDevice(msg);
                // 尝试推送告警信息
                MqMessage mqMessage = pushAlarmIfNeeded(msg, device);
                if (mqMessage != null) {
                    processInfo = ProcessInfo.newInstance(
                            device,
                            JSON.toJSONString(msg),
                            MsgConstants.MSG_TYPE_ALARM,
                            Collections.singletonList(mqMessage)
                    );
                }
                break;
            }
            // 监测消息
            case MkConsts.MSG_TYPE_MONITOR: {
                // 同步设备
                ZaSysDevice device = syncDevice(msg);
                // 推送业务巡检消息
                processInfo = ProcessInfo.newInstance(
                        device,
                        JSON.toJSONString(msg),
                        MsgConstants.MSG_TYPE_BUSINESS,
                        Collections.singletonList(MqMessage.createBusiness(device, JSON.toJSONString(msg)))
                );
            }
        }

        return processInfo;
    }

    private MqMessage pushAlarmIfNeeded(MkCanMsg msg, ZaSysDevice device) {
        String alarmCode = msg.getValue();
        String alarmKey = msg.getDeviceCode() + "_" + alarmCode;
        Long lastAlarmTime = alarmTimeMap.getOrDefault(alarmKey, 0L);
        MqMessage mqMessage = null;
        // 同一设备 2小时内不允许出现重复的告警
        if (lastAlarmTime < DateUtil.offsetHour(new Date(), -2).getTime()) {
            mqMessage = genAlarm(device, msg.getValue(), JSON.toJSONString(msg));
            alarmTimeMap.put(alarmKey, new Date().getTime());
        } else {
            log.warn("铭控设备({})在2小时内出现了重复的告警,已忽略!", msg.getDeviceCode());
        }
        return mqMessage;
    }

    private ZaSysDevice syncDevice(MkCanMsg msg) {
        return DeviceUtil.syncDevice(
                SyncDevice.builder().
                        code(msg.getDeviceCode())
                        .pfCode(CODE)
                        .ip(msg.getSourceIp())
                        .typeCode(DEVICE_TYPE_CODE)
                        .model(DEVICE_MODEL_S905R)
                        .wireless(Constants.YES)
                        .name(msg.getDeviceCode())
                        .online(Constants.YES)
                        .remark(JSON.toJSONString(msg))
                        .build(), this
        );
    }
}
