package com.zhian.gateway.third.common.util;

import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.MessageConstants;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * 设备相关工具类 ， 所有涉及到设备同步的操作全部集成到此类中实现，包含:
 * 1. 设备新增
 * 2. 设备更新
 * 3. 设备删除
 * 4. 设备在线
 * 5. 设备离线
 *
 * @author tongwenjin
 * @since 2024 /8/6
 */
@Slf4j
public class DeviceUtil {

    private static final String DEVICE_COMM_TIME = "device_comm_time";

    /**
     * 同步设备方法, 支持设备状态更新、设备新增、设备状态同步
     *
     * @param syncDevice the sync device
     * @return the za sys device
     */
    public static ZaSysDevice syncDevice(SyncDevice syncDevice, BasePlatformHandler<?> handler) {
        boolean isUpdate = false, updated = false;
        // 保存当前设备数据通讯时间
        IZaSysDeviceService deviceService = SpringUtils.getBean(IZaSysDeviceService.class);
        ZaSysDevice device = deviceService.selectZaSysDeviceByCode(syncDevice.getCode(), syncDevice.getNet());

        // 设备不为空 - 尝试更新部分字段
        if (device != null) {
            isUpdate = true;
            // 设备IP
            if (
                    syncDevice.getId() != null && (device.getIp() == null ||
                            !device.getIp().equalsIgnoreCase(syncDevice.getIp()))
            ) {
                device.setIp(syncDevice.getIp());
                updated = true;
            }
            // 设备备注
            if (
                    syncDevice.getRemark() != null && (device.getRemark() == null ||
                            !device.getRemark().equalsIgnoreCase(syncDevice.getRemark()))
            ) {
                device.setRemark(syncDevice.getRemark());
                updated = true;
            }
            // 设备业务ID
            if (
                    syncDevice.getId() != null && (device.getBizId() == null ||
                            !device.getBizId().equalsIgnoreCase(syncDevice.getId() + ""))
            ) {
                device.setBizId(syncDevice.getId() + "");
                updated = true;
            }
            // 设备型号
            if (
                    syncDevice.getModel() != null &&
                            (device.getModel() == null || !device.getModel().equalsIgnoreCase(syncDevice.getModel()))
            ) {
                device.setModel(syncDevice.getModel());
                updated = true;
            }
            // 设备状态
            if (
                    syncDevice.getOnline() != null && (device.getOnline() == null ||
                            !device.getOnline().equalsIgnoreCase(syncDevice.getOnline()))
            ) {
                device.setOnline(syncDevice.getOnline());
                updated = true;
            }
        } else {
            // 设备为空 - 执行新增设备
            device = new ZaSysDevice();
            device.setName(syncDevice.getName());
            device.setModel(syncDevice.getModel());
            device.setType(syncDevice.getTypeCode());
            device.setPfCode(syncDevice.getPfCode());
            device.setWireless(syncDevice.getWireless());
            device.setNet(syncDevice.getNet());
            device.setCode(syncDevice.getCode());
            device.setIp(syncDevice.getIp());
            device.setRemark(syncDevice.getRemark());
            device.setBizId(syncDevice.getId() + "");
        }

        // 推送设备变化消息
        if (isUpdate && updated) {
            deviceService.updateZaSysDevice(device);
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDevice(
                            device ,
                            MessageConstants.MSG_TYPE_DEVICE_UPD,
                            handler.getPlatform() + " 发起同步"
                    )
            );
            log.info("发现设备({})发生变化,推送设备更新消息!" , device.getCode() );
        }
        else if (!isUpdate) {
            log.info("发现新设备({}),推送设备新增消息!" , device.getCode() );
            deviceService.insertZaSysDevice(device);
            MsgProcessContext.addMsg(
                    MessageBuilder.buildDevice(
                            device ,
                            MessageConstants.MSG_TYPE_DEVICE_ADD ,
                            handler.getPlatform() + " 发起同步"
                    )
            );
        }

        // 主动同步在线设备状态
        long ct = System.currentTimeMillis();
        if (StrUtil.equals(device.getOnline(), Constants.YES)) {
            // 设备在线 - 心跳时间超过1小时
            Long last = getCommTime(device.getId());
            if (last != null && last + 3600_000 < ct) {
                // 上线消息
                log.info("发现设备心跳恢复事件,主动推送设备在线消息:{}", device.getCode());
                MsgProcessContext.addMsg(
                        MessageBuilder.buildDeviceState(
                                device , "1" , "设备恢复心跳 自动同步为在线"
                        )
                );
            }
        }

        // 保存通讯时间
        setCommTime(device.getId());

        return device;
    }


    /**
     * 生成设备CRUD事件
     *
     * @param req the msg data
     * @return the mq message
     */
    public static MqMessage genMessage(DeviceUpdReq req) {
        ZaSysDevice device = req.getDevice();
        String remark = req.getRemark();

        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setType(device.getType());
        facility.setCode(device.getCode());
        facility.setName(device.getName());
        facility.setModel(device.getModel());
        facility.setNet(device.getNet());
        facility.setWireless(StrUtil.equals(device.getWireless(), "1"));
        facility.setOnLine(StrUtil.equals(device.getOnline(), "1"));

        // 设备
        MqMessage message = MqMessage.createDevice(
                device.getId(), "device", facility, remark
        );
        message.setEventType(req.getAction());

        return message;
    }

    public static void setCommTime(Long deviceId) {
        Cache cache = SpringUtils.getBean(Cache.class);
        cache.setCacheMapValue(DEVICE_COMM_TIME, deviceId + "", System.currentTimeMillis());
    }

    public static Long getCommTime(Long deviceId) {
        Cache cache = SpringUtils.getBean(Cache.class);
        return cache.getCacheMapValue(DEVICE_COMM_TIME, deviceId + "");
    }
}
