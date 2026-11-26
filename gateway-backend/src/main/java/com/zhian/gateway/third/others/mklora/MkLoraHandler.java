package com.zhian.gateway.third.others.mklora;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.others.mklora.constants.MkLoraConsts;
import com.zhian.gateway.third.others.mklora.protocol.MkLoraTcpServer;
import com.zhian.gateway.third.others.mklora.vo.MkLoraReportMsg;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 铭控 LoRa 智能无线终端对接（仅主动上报解析）。
 *
 * <p>平台代码：{@code mkLora}。监听端口取自平台 port，未配置时默认 9211。</p>
 * <p>LoRa 网关设备编码：{@code mklora} + 网关 IP，类型 {@code LoraWLG}；
 * 其下终端设备的 {@code net} 指向该网关编码。</p>
 */
@Slf4j
@Component
public class MkLoraHandler extends BasePlatformHandler<MkLoraReportMsg> {

    private final Map<String, Long> alarmTimeMap = new HashMap<>();

    private volatile MkLoraTcpServer server;

    @Override
    public synchronized boolean start(ZaSysPlatform platform) {
        stop();
        try {
            int port = resolvePort(platform);
            server = new MkLoraTcpServer(port, this::processReport);
            server.start();
            log.info("铭控LoRa对接已启动, platform={}, port={}", platform.getCode(), port);
            return true;
        } catch (Exception e) {
            log.error("铭控LoRa TCP 服务启动失败", e);
            server = null;
            return false;
        }
    }

    @Override
    public synchronized boolean stop() {
        MkLoraTcpServer current = server;
        server = null;
        if (current != null) {
            current.stop();
        }
        return true;
    }

    @Override
    public boolean isAlive() {
        MkLoraTcpServer current = server;
        return current != null && current.isActive();
    }

    @Override
    public String getPlatform() {
        return MkLoraConsts.PLATFORM_CODE;
    }

    @Override
    public String getProtocol() {
        return MkLoraConsts.PROTOCOL_CODE;
    }

    @Override
    public R control(ControlVo controlVo) {
        return R.error("铭控LoRa当前仅支持上报解析，暂不支持设备配置/反控");
    }

    @Override
    public ProcessInfo doProcessMsg(MkLoraReportMsg msg) {
        processReport(msg);

        return null;
    }

    private void processReport(MkLoraReportMsg msg) {
        if (msg == null) {
            return;
        }
        String gatewayCode = resolveGatewayCode(msg.getSourceIp());
        ZaSysDevice gateway = syncGateway(gatewayCode, msg.getSourceIp());
        ZaSysDevice device = syncTerminal(msg, gatewayCode);

        JSONObject payload = JSONObject.parseObject(JSON.toJSONString(msg));
        payload.put("gatewayCode", gatewayCode);
        if (gateway != null) {
            payload.put("gatewayId", gateway.getId());
        }

//        consumeMsg(MqMessage.createBusiness(
//                device.getId(),
//                MkLoraConsts.PROTOCOL_CODE,
//                MqMessage.createFacility(device),
//                payload.toJSONString()
//        ));
//        if (msg.isAlarmed()) {
//            pushAlarmIfNeeded(msg, device);
//        }
    }

    private void pushAlarmIfNeeded(MkLoraReportMsg msg, ZaSysDevice device) {
        String alarmCode = String.format("%02X", msg.getAlarmRaw());
        String alarmKey = msg.getDeviceCode() + "_" + alarmCode;
        Long lastAlarmTime = alarmTimeMap.getOrDefault(alarmKey, 0L);
        if (lastAlarmTime < DateUtil.offsetHour(new Date(), -2).getTime()) {
//            pushAlarm(device, alarmCode, JSON.toJSONString(msg));
            alarmTimeMap.put(alarmKey, System.currentTimeMillis());
        } else {
            log.warn("铭控LoRa设备({}) 2小时内重复报警已忽略, alarm={}", msg.getDeviceCode(), alarmCode);
        }
    }

    /**
     * 同步 LoRa 网关设备：编码 mklora + IP，类型 LoraWLG。
     */
    private ZaSysDevice syncGateway(String gatewayCode, String sourceIp) {
        if (StrUtil.isEmpty(gatewayCode)) {
            return null;
        }
        return DeviceUtil.syncDevice(SyncDevice.builder()
                .code(gatewayCode)
                .name("LoRa网关-" + (StrUtil.isEmpty(sourceIp) ? gatewayCode : sourceIp))
                .net(gatewayCode)
                .pfCode(MkLoraConsts.PLATFORM_CODE)
                .ip(sourceIp)
                .typeCode(MkLoraConsts.GATEWAY_TYPE_CODE)
                .wireless(Constants.YES)
                .online(Constants.YES)
                .remark("铭控LoRa网关")
                .build());
    }

    /**
     * 同步终端设备，net 挂到所属 LoRa 网关编码。
     */
    private ZaSysDevice syncTerminal(MkLoraReportMsg msg, String gatewayCode) {
        return DeviceUtil.syncDevice(SyncDevice.builder()
                .code(msg.getDeviceCode())
                .name(msg.getSensorTypeName() + "-" + msg.getDeviceCode())
                .net(gatewayCode)
                .pfCode(MkLoraConsts.PLATFORM_CODE)
                .ip(msg.getSourceIp())
                .typeCode(MkLoraConsts.DEVICE_TYPE_CODE)
                .model(msg.getSensorTypeName())
                .wireless(Constants.YES)
                .online(Constants.YES)
                .remark("type=" + msg.getSensorType() + ", signal=" + msg.getSignalDbm() + "dBm")
                .build());
    }

    static String resolveGatewayCode(String sourceIp) {
        if (StrUtil.isEmpty(sourceIp)) {
            return null;
        }
        return MkLoraConsts.GATEWAY_CODE_PREFIX + sourceIp.replace("." , "").trim();
    }

    private int resolvePort(ZaSysPlatform platform) {
        if (platform != null && platform.getPort() != null && platform.getPort() > 0) {
            return platform.getPort();
        }
        return MkLoraConsts.DEFAULT_PORT;
    }
}
