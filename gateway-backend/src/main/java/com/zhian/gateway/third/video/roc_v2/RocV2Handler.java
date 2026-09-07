package com.zhian.gateway.third.video.roc_v2;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.base.BaseException;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.PluginHealthResult;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.utils.SerialPortUtil;
import com.zhian.gateway.third.video.roc.RocHandler;
import com.zhian.gateway.third.video.roc.event.RocFaceEvent;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("CallToPrintStackTrace")
@Component
@Slf4j
public class RocV2Handler extends BasePlatformHandler<Object> {

    public static final String PLATFORM_NAME = "roc_v2";

    public static final String PROTOCOL_NAME = "roc_v2";
    // 目标 WebSocket 服务器地址
    public static ZaSysPlatform platform;
    private static Boolean running = false;

    public static FmConnector connector;
    private final RocHandler rocHandler;

    private ZaSysDevice moduleDevice;

    public RocV2Handler(RocHandler rocHandler) {
        super();
        this.rocHandler = rocHandler;
    }

    @Override
    public boolean start(ZaSysPlatform platform) {
        log.info("启动人脸识别服务(V2)");
        RocV2Handler.platform = platform;

        // 初始化对接服务
        String deviceName = SerialPortUtil.searchFmDevice();
        FmConnector.connectPort(deviceName);
        // 检查服务状态
        SerialPortUtil.powerOn();
        running = StrUtil.isNotBlank(FmConnector.getSn());
        // 同步人脸摄像头
        if (running) {
            syncModuleDevice();
        }
        SerialPortUtil.powerOff();

        return running;
    }

    @Override
    public boolean stop() {
        log.info("停止人脸识别服务");
        running = false;
        FmConnector.closePort();
        return true;
    }

    @Override
    public boolean isAlive() {
        return running && FmConnector.isOpen();
    }

    @Override
    public PluginHealthResult checkHealth() {
        return isAlive()
                ? PluginHealthResult.healthy("串口连接正常，设备序列号 " + FmConnector.getSn())
                : PluginHealthResult.unhealthy("串口连接已断开");
    }

    /**
     * 推送人脸识别设备到业务平台
     */
    public synchronized void syncModuleDevice() {
        String sn = FmConnector.getSn();

        ZaSysDevice sysDevice = deviceService.selectByCode(sn, PLATFORM_NAME);
        if (sysDevice == null) {
            sysDevice = new ZaSysDevice();
            sysDevice.setType(PLATFORM_NAME);
            sysDevice.setOnline("1");
            sysDevice.setIp(platform.getIp());
            sysDevice.setCode(sn);
            // sysDevice.setNet(PLATFORM_NAME);
            sysDevice.setName("人脸识别模块V2(" + sn + ")");
            sysDevice.setPfCode(PLATFORM_NAME);
            sysDevice.setModel("roc_v2");
            sysDevice.setWireless("0");
            deviceService.insertZaSysDevice(sysDevice);
        } else if (!sysDevice.getIp().equalsIgnoreCase(platform.getIp())) {
            sysDevice.setIp(platform.getIp());
            deviceService.updateZaSysDevice(sysDevice);
        }

        //推送设备信息
        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setType(sysDevice.getType());
        facility.setPfCode(getPlatform());
        facility.setCode(sysDevice.getCode());
        facility.setName(sysDevice.getName());
        facility.setModel(sysDevice.getModel());
        facility.setNet(sysDevice.getNet());
        facility.setWireless(sysDevice.getWireless() != null && sysDevice.getWireless().equalsIgnoreCase("1"));
        facility.setOnLine(sysDevice.getOnline() != null && sysDevice.getOnline().equalsIgnoreCase("1"));
        MqMessage message = MqMessage.createDevice(sysDevice.getId(), getProtocol(), facility, sysDevice.getRemark());
        message.setEventType(MqMessage.DEVICE_ADD);
        log.info("同步人脸模块设备信息至消安...");
        consumeMsg(message);

        moduleDevice = sysDevice;
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }


    /**
     * 提供子类实现msgObj自动类型转换
     *
     * @param msgObj the msg obj
     */
    public ProcessInfo doProcessMsg(Object msgObj) {
        throw new UnsupportedOperationException("人脸识别模块V2暂不支持事件回调!");
    }


    @Override
    public R doControl(ControlVo controlVo) {
        // 拉流
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            SerialPortUtil.powerOn();
            return R.success(FmConnector.fetchStream());
        }
        // 处理人脸相关业务
        else if (ControlVo.CMD_SET.equalsIgnoreCase(controlVo.getCommand())) {
            SerialPortUtil.powerOn();
            RocFaceEvent event = JSONObject.parseObject(controlVo.getValue(), RocFaceEvent.class);
            R result = handleFaceEvent(event);
            if (!result.get("msg").toString().contains("模块执行中") && !event.getAction().equals("delete")) {
                // 延迟一秒断电
                SerialPortUtil.powerOff(1000L);
            }
            return result;
        }
        // 电源相关操作
        else if (ControlVo.CMD_POWER.equals(controlVo.getCommand())) {
            if ("off".equals(controlVo.getValue())) {
                SerialPortUtil.powerOff();
                FmConnector.closeStream();
                log.info("电源关闭指令执行成功!");
            }
            return R.success();
        } else {
            return R.error("不支持的操作");
        }
    }

    private R handleFaceEvent(RocFaceEvent faceEvent) {
        // 处理人脸事件
        switch (faceEvent.getAction()) {
            // 执行人脸识别
            case RocFaceEvent.SCAN: {
                String faceId = "";
                String msg = "";
                try {
                    faceId = FmConnector.runWithLock(FmConnector::verify);
                    msg = "OK";
                } catch (Exception e) {
                    if (e instanceof ExecutionException || e instanceof InterruptedException || e instanceof TimeoutException) {
                        msg = "执行失败,操作超时!";
                    }
                    else if (e instanceof BaseException) {
                        log.warn("执行失败,{}", e.getMessage());
                        msg = e.getMessage();
                    }
                    else {
                        msg = e.getMessage();
                        log.error("执行失败,{}", e.getMessage());
                    }
                }

                return StrUtil.isBlank(faceId) ? R.error(msg) : R.success("OK", faceId);
            }
            // 清空人脸数据
            case RocFaceEvent.CLEAR: {
                FmConnector.delAllUser();
                SerialPortUtil.powerOff();
                R.success("OK");
                break;
            }
            // 删除指定人脸数据
            case RocFaceEvent.DELETE: {
                boolean deleted = false;
                String msg = "";

                try {
                    deleted = FmConnector.runWithLock(() -> FmConnector.delById(faceEvent.getResult()));
                    msg = "OK";
                } catch (Exception e) {
                    if (e instanceof ExecutionException || e instanceof InterruptedException || e instanceof TimeoutException) {
                        msg = "操作失败,执行超时!";
                    } else if (e instanceof BaseException) {
                        msg = e.getMessage();
                    } else {
                        log.error("执行异常,{}", e.getMessage());
                        e.printStackTrace();
                    }
                }
                return StrUtil.equals(msg, "OK") ? R.success(msg) : R.error(msg);
            }
            // 录入人脸数据
            case RocFaceEvent.ADD: {
                String userName = faceEvent.getName();
                String faceImg = faceEvent.getFaceImg();

                Integer faceId = null;

                // 无照片-通过识别录入
                String msg = "";
                if (StrUtil.isBlank(faceImg)) {
                    try {
                        faceId = FmConnector.runWithLock(() -> FmConnector.enrollSingle(userName));
                    } catch (Exception e) {
                        if (e instanceof ExecutionException || e instanceof TimeoutException || e instanceof InterruptedException) {
                            msg = "录入超时!";
                            SerialPortUtil.powerOff();
                        } else if (e instanceof BaseException) {
                            msg = e.getMessage();
                        } else {
                            log.error("执行发生异常,{}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                // 通过照片录入
                else {
                    faceId = FmConnector.enrollPic(faceImg, userName);
                }

                return faceId != null ? R.success("OK", faceId) : R.error("操作失败:" + msg);
            }
        }

        return R.success("OK");
    }
}
