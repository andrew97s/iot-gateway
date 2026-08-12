package com.zhian.gateway.third.jadebird;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.*;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.DeviceConstants;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.sys.domain.ZaDeviceType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.jadebird.util.JbProtocolParser;
import com.zhian.gateway.third.jadebird.vo.Facility;
import com.zhian.gateway.third.jadebird.vo.HrpDeivceVo;
import com.zhian.gateway.third.jadebird.vo.JbResponse;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import com.zhian.gateway.third.utils.RabbitMqUtil;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

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
    private static ZaSysPlatform zaSysPlatform;
    private static Channel channel;
    private static Boolean running = false;
    private static Boolean useMQ = false;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        JadebirdInnerHandler.zaSysPlatform = zaSysPlatform;
        if (StringUtils.isEmpty(zaSysPlatform.getConfig()) || StringUtils.isEmpty(zaSysPlatform.getConfigStr("ip"))) {
            log.info("未配置RabbitMQ，将等待青鸟网关通过HTTP推送过来的数据");
            running = true;
        } else {
            useMQ = true;
            log.info("已配置RabbitMQ： {}，将尝试订阅 {} 数据", zaSysPlatform.getConfigStr("ip"), zaSysPlatform.getConfigStr("vhost"));
            running = consumeMQ();
        }

        return running;
    }

    /**
     * 消费MQ
     *
     * @return
     */
    private boolean consumeMQ() {
        String ip = zaSysPlatform.getConfigStr("ip");
        Integer port = zaSysPlatform.getConfigInt("port");
        // 为防止dev 与 test 环境互相影响 ， 此处应通过 profiles 来区分 vhost
        String vhost = Arrays.asList(SpringUtils.getActiveProfiles()).contains("test") ?
                "test" : zaSysPlatform.getConfigStr("vhost");
        String username = zaSysPlatform.getConfigStr("username");
        String password = zaSysPlatform.getConfigStr("password");
        Connection connection = RabbitMqUtil.getConnection(ip, port, vhost, username, password);
        if (connection == null) {
            log.error("连接RabbitMQ失败");
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "连接RabbitMQ异常", null, zaSysPlatform.getConfig());
            return false;
        }

        DeliverCallback deliverCallback = (tag, message) -> {
            //将信道存储的信息转化为字符串类型
            String msg = new String(message.getBody());
            log.info("receive rabbit {} message: {}", zaSysPlatform.getName(), msg);

            String results = "Y";
            int log = 0;
            MonitorMsg monitorMsg = JSONObject.parseObject(msg, MonitorMsg.class);
            try {
                processMsg(monitorMsg);
            } catch (Exception e) {
                e.printStackTrace();
                log = zaSysErrorService.log(ZaSysError.TYPE_MQ, PLATFORM_NAME, "消息处理失败: " + e.getMessage(), msg);
                results = "N";
            } finally {
                ZaSysDevice device = MessageUtil.getDevice();
                //记录消息
                if (device != null) {
                    zaSysMessageService.log(MessageUtil.getDevice(), monitorMsg.getEvent(), msg, results);
                    MessageUtil.clear();
                } else if (monitorMsg.getEvent() != null && !monitorMsg.getEvent().equalsIgnoreCase(MonitorMsg.Event.HEARTBEAT.name()) && log == 0) {
                    zaSysErrorService.log(ZaSysError.TYPE_MQ, getPlatform(), "消息被忽略", msg);
                }

                channel.basicAck(message.getEnvelope().getDeliveryTag(), true);
            }
        };
        //取消消费的回调接口
        CancelCallback cancelCallback = (tag) -> {
            log.info("rabbit {} message {} cancel", zaSysPlatform.getName(), tag);
        };
        ShutdownListener shutdownListener = (ShutdownSignalException cause) -> {
            log.error("RabbitMQ {} 连接异常断开: {}", ip, cause);
        };
        String exchange = zaSysPlatform.getConfigStr("exchange", "monitor.src.upload");
        String queue = zaSysPlatform.getConfigStr("queue", "zhian");
        String key = zaSysPlatform.getConfigStr("key", "");
        channel = RabbitMqUtil.consume(connection, exchange, queue, key, deliverCallback, cancelCallback, shutdownListener);
        RabbitMqUtil.consume(channel, "monitor.src.link", "za-heart", key, deliverCallback, cancelCallback);
        if (channel == null) {
            log.error("绑定RabbitMQ消费队列失败");
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "绑定RabbitMQ消费队列失败", RabbitMqUtil.getException().getMessage(), zaSysPlatform.getConfig());
            return false;
        }
        return true;
    }

    @Override
    public boolean stop() {
        log.info("将忽略青鸟云平台推送过来的数据");
        if (!useMQ) {
            return true;
        }
        try {
            RabbitMqUtil.close(zaSysPlatform.getConfigStr("ip"));
            //channel.close();
            channel = null;
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     *
     * @return
     */
    public boolean isAlive() {
        return running;
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
        uri = "http://" + zaSysPlatform.getIp() + ":" + zaSysPlatform.getPort() + uri;
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
        if (!running) {
            log.error("青鸟网关插件暂时停止");
            return null;
        }

        MonitorMsg monitorMsg = (msgObj instanceof MonitorMsg) ?
                (MonitorMsg) msgObj : JSONObject.parseObject(msgObj.toString(), MonitorMsg.class);
        Facility mf = monitorMsg.getFacility();
        if (mf == null ) {
            throw new ServiceException("数据格式有误");
        }
        String addrStr = mf.getAddrStr();
        String net = mf.getNet();
        if (StrUtil.isBlank(addrStr)) {
            throw new ServiceException("数据格式有误");
        }
        // 网关编码自动转小写
        else {
            if (StrUtil.equals(addrStr , mf.getNet())) {
                mf.setAddrStr(addrStr.toLowerCase());
            }
            mf.setNet(StrUtil.isNotBlank(net) ? net.toLowerCase() : "");
        }

        // 当前设备归属的网关设备编码,生成网关设备
        ZaSysDevice sysDevice = syncNet(mf, JSONObject.toJSONString(msgObj));
        if (sysDevice == null) {
            log.error("同步网关设备失败： {}", mf.getNet());
            return null;
        }

        String code = addrStr2Code(addrStr);
        if (mf.getNet() != null && !StrUtil.equals(code , net)) {
            sysDevice = syncFacility(mf, JSONObject.toJSONString(msgObj));
        }

        //心跳数据，包括巡检
        if (monitorMsg.getEvent().equalsIgnoreCase(MonitorMsg.Event.HEARTBEAT.name())) {
            if (
                    mf.getAnalogValue() == null &&
                            mf.getTemperature() == null &&
                            mf.getRssi() == null &&
                            mf.getVoltage() == null
            ) {
                log.debug("无监测值的心跳数据，暂时忽略");
                return null;
            }
        }

        // 当前设备编码，如果以”通道“结尾，就去掉通道号
        String fsn = addrStr.trim();

        Integer chn = null;
        if (fsn.endsWith("通道")) {
            chn = Integer.parseInt(fsn.substring(fsn.indexOf(' ') + 1).substring(0, 1));
        }
        if (fsn.contains(" 线路") && mf.getNet() != null && fsn.startsWith(mf.getNet())) {
            chn = Integer.parseInt(fsn.substring(fsn.indexOf("线路") + 2));
        }
        // 剔除 【通道 、 线路】 关键字
        if (fsn.contains("通道") || fsn.contains("线路")) {
            fsn = fsn.substring(0, fsn.lastIndexOf(' ')).trim();
        }

        MessageUtil.setDevice(sysDevice);

        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setCode(fsn);
        facility.setOnLine(true);
        facility.setWireless(mf.isWireless());
        facility.setName(sysDevice.getName());
        facility.setNet(mf.getNet());
        facility.setChn(chn);
        facility.setModel(StringUtils.isEmpty(mf.getFacilitiesModel()) ? mf.getModel() : mf.getFacilitiesModel());
        facility.setType(mf.getFacilitiesTypeCode().toString());
        if (StringUtils.isNotEmpty(mf.getRssi())) {
            facility.setRssi(Integer.parseInt(mf.getRssi()));
        }
        if (StringUtils.isNotEmpty(mf.getTemperature())) {
            facility.setTemperature(Integer.parseInt(mf.getTemperature()));
        }
        if (StringUtils.isNotEmpty(mf.getVoltage())) {
            facility.setVoltage(Integer.parseInt(mf.getVoltage()));
        }

        // 解析告警 & 业务监测数据
        MsgProcessContext.getProcessInfo().setDevice(sysDevice);
        List<Message> msgList = JbProtocolParser.extractMessage(sysDevice, monitorMsg);

        MsgProcessContext.addMsg(msgList);
        return null;
    }

    /**
     * 地址转换成设备代码
     *
     * @param addStr
     * @return
     */
    private String addrStr2Code(String addStr) {
        if (addStr.contains("机")) {
            return addStr.substring(0, addStr.indexOf("机") + 1);
        } else if (addStr.contains("通道")) {
            return addStr.substring(0, addStr.lastIndexOf(' ')).trim();
        } else if (addStr.contains("线路")) {
            return addStr.substring(0, addStr.lastIndexOf(' ')).trim();
        } else {
            return addStr;
        }
    }

    /**
     * 同步注册网关设备信息
     *
     * @param mf
     * @param msg
     * @return
     */
    private ZaSysDevice syncNet(Facility mf, String msg) {
        String net = mf.getNet();
        // TR_SERVER 此处一定包含网关字段
        if (StringUtils.isEmpty(net)) {
            log.error("网关不能为空");
            return null;
        }

        String typeCode = "128";
        net = net.toLowerCase();
        String name = "网关" + net;
        // 用传(主机) 有线
        if (isUITD(net)) {
            typeCode = DeviceConstants.UITD;
            name = "用传" + net;
        }
        // HRP（路由） 无线
        else {
            typeCode = DeviceConstants.HRPWLG;
        }
        // 同步设备信息
        SyncDevice syncDevice = SyncDevice.builder()
                .code(net)
                .name(name)
                .net(net)
                .model(mf.getFacilitiesModel())
                .typeCode(typeCode)
                .pfCode(zaSysPlatform.getCode())
                .wireless(typeCode.equals(DeviceConstants.HRPWLG) ? "1" : "0")
                .build();
        return DeviceUtil.syncDevice(syncDevice, this);
    }

    /**
     * 同步设备信息,不包括网关
     *
     * @param mf mf
     * @param msg msg
     */
    private ZaSysDevice syncFacility(Facility mf, String msg) {
        // 网关编码
        String net = StrUtil.isBlank(mf.getNet()) ? "" : mf.getNet().trim().toLowerCase();
        String code = mf.getAddrStr();

        // 当前设备编码，如果以”通道“结尾，就去掉通道号
        String fsn = mf.getAddrStr() == null ? "" : mf.getAddrStr().trim();

        // 解析设备类型
        TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaDeviceType> mfType = typeMapping.resolveDeviceType(getPlatform(), mf.getFacilitiesTypeCode() + "");
        String typeCode = "UNKNOWN";
        if (mfType.isPresent()) {
            typeCode = mfType.get().getCode();
        }

        // 主机设备信息
        if (fsn.contains("机")) {
            code = code.substring(0, code.indexOf("机") + 1);
            ZaSysDevice ctlDevice = deviceService.selectZaSysDeviceByCode(code, net);
            if (ctlDevice == null) {
                // 同步设备信息
                SyncDevice syncDevice = SyncDevice.builder()
                        .id(mf.getId())
                        .code(code)
                        .name(code)
                        .net(net)
                        .typeCode(mf.getFacilitiesTypeCode() == null ? "FAC" : typeCode)
                        .pfCode(zaSysPlatform.getCode())
                        .wireless("0")
                        .build();
                ctlDevice = DeviceUtil.syncDevice(syncDevice, this);
            }
            //当前是主机设备
            if (mf.getFacilitiesTypeCode() == 1) {
                return ctlDevice;
            }
        }

        //同步部件信息
        code = mf.getAddrStr();
        if (code.contains("通道") || code.contains("线路")) {
            code = code.substring(0, code.lastIndexOf(' ')).trim();
        }
        String name = StringUtils.isNotEmpty(mf.getDescr()) ? mf.getDescr() : code;

        ZaSysDevice componentDevice = deviceService.selectZaSysDeviceByCode(code, net);
        if (componentDevice == null) {
            // 同步设备信息
            SyncDevice syncDevice = SyncDevice.builder()
                    .id(mf.getId())
                    .code(code)
                    .name(name)
                    .net(net)
                    .model(StringUtils.isEmpty(mf.getFacilitiesModel()) ? mf.getModel() : mf.getFacilitiesModel())
                    .typeCode(typeCode)
                    .pfCode(zaSysPlatform.getCode())
                    .wireless(mf.isWireless() ? "1" : "0")
                    .build();
            componentDevice = DeviceUtil.syncDevice(syncDevice, this);
        }

        return componentDevice;
    }

    /**
     * Is uitd boolean.
     *
     * @param code the code
     * @return the boolean
     */
    public static boolean isUITD(String code){
        return code.length() == 32 && code.startsWith("000000");
    }
}
