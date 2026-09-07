package com.zhian.gateway.third.jadebird;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.*;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.consts.DeviceConstants;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.PluginHealthResult;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.jadebird.util.JbMqUtil;
import com.zhian.gateway.third.jadebird.util.JbProtocolParser;
import com.zhian.gateway.third.jadebird.vo.HrpDeivceVo;
import com.zhian.gateway.third.jadebird.vo.JbResponse;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import com.zhian.gateway.third.utils.RabbitMqUtil;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 青鸟云平台对接处理器，处理通过RabbitMQ或HTTP接口接收消息
 * 1、心跳数据，注册用传128或HRP网关设备信息，判断当前传输设备是否在线
 * 2、告警数据，上报告警信息
 * 3、处理设备信息时，先注册网关设备，如果本地已经注册，则定时向上传上报在线或者离线的心跳，保证数据和状态的一致性
 * 4、如果是主机（类型是1）上报信息，就不处理部件
 */
@Component("jadebirdInnerHandler")
@Slf4j
@DependsOn(value = "gatewayHandler")
public class JadebirdInnerHandler extends BasePlatformHandler {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "trserver";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "jb";
    /**
     * The constant CACHE_MAP.
     */
    public static final String CACHE_MAP = "trserver";
    /**
     * The constant OFFLINE_HOURS.
     */
    public static final Integer OFFLINE_HOURS = 1;

    private static Boolean useMQ = false;
    private static final int LOG_BODY_LIMIT = 512;

    @Override
    public boolean start(ZaSysPlatform platform) {
        super.start(platform);
        if (StrUtil.equals(platform.getConfigStr("useMQ") , "false")) {
            useMQ = false;
            log.info("未配置RabbitMQ，将等待青鸟网关通过HTTP推送过来的数据");
        }
        else {
            useMQ = true;
            log.info(
                    "已配置RabbitMQ： {}，将尝试订阅 {} 数据",
                    platform.getConfigStr("ip"), platform.getConfigStr("vhost")
            );

            JbMqUtil.subscribeMessage(this::handleDelivery, platform);
            JbMqUtil.subscribeHeart(this::handleDelivery, platform);

        }
        running = true;

        return running;
    }

    private void handleDelivery(Channel channel, Delivery message) {
        long deliveryTag = message.getEnvelope().getDeliveryTag();
        boolean redelivered = message.getEnvelope().isRedeliver();
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        if (log.isDebugEnabled()) {
            log.debug("receive rabbit {} message: {}", platform.getName(), msg);
        } else {
            log.info("receive rabbit {} message, size={}, body={}", platform.getName(), msg.length(), truncate(msg));
        }

        try {
            MonitorMsg monitorMsg = JSONObject.parseObject(msg, MonitorMsg.class);
            if (monitorMsg == null) {
                throw new ServiceException("消息体为空");
            }
            processMsg(monitorMsg);
            RabbitMqUtil.ack(channel, deliveryTag);
        } catch (ServiceException e) {
            log.error("消息无法处理, drop: {}", e.getMessage());
            RabbitMqUtil.nack(channel, deliveryTag, false);
        } catch (Exception e) {
            log.error("处理消息发生异常: {}", e.getMessage(), e);
            // 首次失败重投，再次失败则丢弃，避免消息死循环
            RabbitMqUtil.nack(channel, deliveryTag, !redelivered);
        } finally {
            MessageUtil.clear();
        }
    }

    @Override
    public PluginHealthResult checkHealth() {
        if (!running) {
            return PluginHealthResult.unhealthy("插件未运行");
        }
        if (!useMQ) {
            return PluginHealthResult.healthy("HTTP被动接收端已就绪");
        }
        return JbMqUtil.isAlive()
                ? PluginHealthResult.healthy(JbMqUtil.connectionInfo())
                : PluginHealthResult.unhealthy(JbMqUtil.connectionInfo());
    }

    private static String truncate(String msg) {
        if (msg == null || msg.length() <= LOG_BODY_LIMIT) {
            return msg;
        }
        return msg.substring(0, LOG_BODY_LIMIT) + "...";
    }

    @Override
    public boolean stop() {
        super.stop();
        JbMqUtil.closeChannel();
        useMQ = false;
        return true;
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
     * 反向控制
     *
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        log.info("control : {}", controlVo);
        if (!running) {
            log.error("青鸟网关插件暂时停止");
            return R.error("插件暂时停止");
        }
        ZaSysDevice device = controlVo.getDevice();
        if (StringUtils.isEmpty(device.getNet())) {
            return R.error("找不到网关");
        }

        JbResponse res = null;

        //用传和HRP网关，支持复位和消音
        if (device.getType().equalsIgnoreCase(DeviceConstants.UITD)) {
            res = controlNet(controlVo);
        } else if (device.getType().equalsIgnoreCase(DeviceConstants.HRPWLG)) {
            res = controlHrp(controlVo);
        } else {
            if (device.getWireless().equalsIgnoreCase("1")) {
                //HRP部件
                res = controlHrp(controlVo);
            } else if (device.getCode().contains("机") && device.getCode().contains("-")) {
                res = controlComponent(controlVo);
            } else {
                res = controlController(controlVo);
            }
        }

        if (res != null && res.isSuccess()) {
            return R.success();
        } else {
            return R.error("发送指令失败");
        }
    }

    /**
     * 反控 HRP网关或用传
     *
     * @param controlVo
     * @return
     */
    private JbResponse controlNet(ControlVo controlVo) {
        Map<String, Object> parammap = new HashMap<>();
        ZaSysDevice device = controlVo.getDevice();
        parammap.put("psn", device.getNet());
        //用传复位/消音
        if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_RESET)) {
            parammap.put("type", 3);
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_MUTE)) {
            parammap.put("type", 1);
        } else {
            throw new ServiceException("不支持的指令");
        }

        return sendRequest("/api/net/remoteControl", parammap);
    }

    /**
     * 反控主机的现场部件
     *
     * @param controlVo
     * @return
     */
    private JbResponse controlComponent(ControlVo controlVo) {
        if (StringUtils.isEmpty(controlVo.getNet())) {
            throw new ServiceException("找不到网关");
        }
        Map<String, Object> parammap = new HashMap<>();
        ZaSysDevice device = controlVo.getDevice();
        //有线部件
        String no = device.getCode();
        int pos = no.indexOf("机");
        if (pos > 0) {
            no = no.substring(0, pos);
        }
        pos = no.indexOf(" ");
        if (pos > 0) {
            no = no.substring(0, pos);
        }
        parammap.put("psn", device.getNet());
        parammap.put("controllerNo", Integer.parseInt(no));

        String uri = null;
        if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_RESET)) {
            uri = "/api/controller/remoteReset";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_MUTE)) {
            uri = "/api/controller/remoteMute";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_POWER)) {
            uri = "/api/device/remoteControl";
            String[] lp = device.getCode().substring(pos + 1).split("-");
            parammap.put("devAddr", String.format("%d%03d%03d", no, Integer.parseInt(lp[0]), Integer.parseInt(lp[1])));

            if (controlVo.getValue().equalsIgnoreCase("1")) {
                parammap.put("stat", 2);
            } else if (controlVo.getValue().equalsIgnoreCase("0")) {
                parammap.put("stat", 3);
            }
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_SHIELD)) {
            uri = "/api/device/remoteControl";
            String[] lp = device.getCode().substring(pos + 1).trim().split("-");
            String devAddr = String.format("%d%03d%03d", Integer.parseInt(no), Integer.parseInt(lp[0]), Integer.parseInt(lp[1]));
            parammap.put("devAddr", Integer.parseInt(devAddr));

            if (controlVo.getValue().equalsIgnoreCase("1")) {
                parammap.put("stat", 12);
            } else if (controlVo.getValue().equalsIgnoreCase("0")) {
                parammap.put("stat", 13);
            }
        } else {
            throw new ServiceException("不支持的指令");
        }
        return sendRequest(uri, parammap);
    }

    /**
     * 反控主机（控制器）
     *
     * @param controlVo
     * @return
     */
    private JbResponse controlController(ControlVo controlVo) {
        if (StringUtils.isEmpty(controlVo.getNet())) {
            throw new ServiceException("找不到网关");
        }
        Map<String, Object> parammap = new HashMap<>();
        ZaSysDevice device = controlVo.getDevice();
        String no = device.getCode();
        int pos = no.indexOf("机");
        if (pos > 0) {
            no = no.substring(0, pos);
        }
        pos = no.indexOf(" ");
        if (pos > 0) {
            no = no.substring(0, pos);
        }
        parammap.put("psn", device.getNet());
        parammap.put("controllerNo", Integer.parseInt(no));
        String uri = null;
        if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_RESET)) {
            uri = "/api/controller/remoteReset";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_MUTE)) {
            uri = "/api/controller/remoteMute";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_MANUAL)) {
            //远程控制气灭控制器气灭区
            String part = controlVo.getCode().substring(controlVo.getCode().indexOf('机') + 1);
            parammap.put("part", Integer.parseInt(part));
            parammap.put("stat", Integer.parseInt(controlVo.getValue()));
            uri = "/api/controller/gasExtinctionZone";
        } else {
            throw new ServiceException("不支持的指令");
        }
        return sendRequest(uri, parammap);
    }

    /**
     * 反控HRP部件
     *
     * @param controlVo
     * @return
     */
    private JbResponse controlHrp(ControlVo controlVo) {
        if (StringUtils.isEmpty(controlVo.getNet())) {
            throw new ServiceException("找不到网关");
        }

        Map<String, Object> parammap = new HashMap<>();
        ZaSysDevice device = controlVo.getDevice();
        //HRP部件
        String uri = null;
        if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_MUTE)) {
            parammap.put("net", device.getNet().toUpperCase());
            parammap.put("imei", device.getCode());
            parammap.put("facilitesType", Integer.parseInt(device.getType()));
            uri = "/api/hrp/mute";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_RESET)) {
            //HRP设置备的复位转换成停止指令
            parammap.put("type", 4);
            HrpDeivceVo hrpDevice = new HrpDeivceVo();
            hrpDevice.setNet(device.getNet().toUpperCase());
            hrpDevice.setImeis(new String[]{device.getCode()});
            parammap.put("devices", new HrpDeivceVo[]{hrpDevice});
            uri = "/api/hrp/linkage";
        } else if (controlVo.getCommand().equalsIgnoreCase(ControlVo.CMD_POWER)) {
            if (controlVo.getValue().equalsIgnoreCase("1")) {
                parammap.put("type", 2);
            } else if (controlVo.getValue().equalsIgnoreCase("0")) {
                parammap.put("type", 4);
            }
            HrpDeivceVo hrpDevice = new HrpDeivceVo();
            hrpDevice.setNet(device.getNet().toUpperCase());
            hrpDevice.setImeis(new String[]{device.getCode()});
            parammap.put("devices", new HrpDeivceVo[]{hrpDevice});
            uri = "/api/hrp/linkage";
        } else {
            throw new ServiceException("不支持的指令");
        }
        return sendRequest(uri, parammap);
    }

    /**
     * 发起tr_server请求
     *
     * @param uri
     * @param paramMap
     * @return
     */
    private JbResponse sendRequest(String uri, Map<String, Object> paramMap) {
        uri = "http://" + platform.getIp() + ":" + platform.getPort() + uri;
        Map<String, String> headMap = new HashMap<>();
        headMap.put("Content-Type", "application/json");
        String resStr = HttpUtils.postJSON(uri, paramMap, headMap);
        log.info("tr_server post{}: {}\r\nresponse: {}", uri, JSONObject.toJSONString(paramMap), resStr);
        return JSONObject.parseObject(resStr, JbResponse.class);
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param msgObj msgObj
     * @return info
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        MonitorMsg monitorMsg = JSONObject.parseObject(msgObj.toString(), MonitorMsg.class);
        ZaSysDevice sysDevice = JbProtocolParser.requireDevice(monitorMsg.getFacility(), getPlatform());

        if (sysDevice == null) {
            log.error("处理青鸟云设备失败,注册设备失败!");
            return null;
        }

        MessageUtil.setDevice(sysDevice);

        // 解析告警 & 业务监测数据
        MsgProcessContext.getProcessInfo().setDevice(sysDevice);
        List<Message> msgList = JbProtocolParser.extractMessage(sysDevice, monitorMsg);

        MsgProcessContext.addMsg(msgList);
        return null;
    }
}
