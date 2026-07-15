package com.zhian.gateway.third.jadebird;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.jadebird.util.JBApiUtil;
import com.zhian.gateway.third.jadebird.vo.BaseResponseInfo;
import com.zhian.gateway.third.jadebird.vo.Facility;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 青鸟云平台对接处理器，通过HTTP接口接收消息 TODO
 */
@Component
@Slf4j
public class JadebirdCloudHandler extends BasePlatformHandler {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "jb";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "jb";
    /**
     * The constant JADE_BIRD_TOKEN.
     */
    public static final String JADE_BIRD_TOKEN = "token:jb";
    public static final String CACHE_MAP = "jb-cloud";
    /**
     * The constant zaSysPlatform.
     */
    private static ZaSysPlatform zaSysPlatform;
    /**
     * The constant running.
     */
    private static Boolean running = false;

    /**
     * The constant JB_API.
     */
    private static JBApiUtil JB_API;
    @Autowired
    private IZaSysDeviceService deviceService;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        JadebirdCloudHandler.zaSysPlatform = zaSysPlatform;
        JB_API = new JBApiUtil(zaSysPlatform);
        log.info("等待青鸟云平台推送过来的数据");
        running = true;
        // 尝试同步平台设备 TODO 同步设备状态
        CompletableFuture.runAsync(() -> syncAllIfNeeded(zaSysPlatform, 1)).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
        return true;
    }

    @Override
    public boolean stop() {
        log.info("将忽略青鸟云平台推送过来的数据");
        running = false;
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
     * 反向控制 TODO
     *
     * @param controlVo
     * @return
     */
    @Override
    public AjaxResult doControl(ControlVo controlVo) {
        // 校验
        if (!isAlive()) {
            log.error("青鸟云插件暂时停止");
            return AjaxResult.error("插件暂时停止");
        }
        ZaSysDevice device = controlVo.getDevice();
        device = deviceService.selectZaSysDeviceByCode(device.getCode(), device.getNet());
        if (device == null) {
            return AjaxResult.error("反控操作失败,device 不能为空!");
        }

        String[] cmds = controlVo.getCommand().split(",");
        for (String cmd : cmds) {
            // 消音
            if ("mute".equals(cmd)) {
                JB_API.mute(device.getBizId(), device.getCode());
            }
            // 复位 - 待实现
            if ("reset".equals(cmd)) {
                log.warn("反控操作暂未实现复位操作!");
            }
        }
        return AjaxResult.success();
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param msgObj
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("青鸟云插件暂时停止");
            return null;
        }
        MonitorMsg monitorMsg = (msgObj instanceof MonitorMsg) ?
                (MonitorMsg) msgObj : JSONObject.parseObject(msgObj.toString(), MonitorMsg.class);
        Facility mf = monitorMsg.getFacility();
        if (mf == null || StringUtils.isEmpty(mf.getAddrStr())) {
            throw new ServiceException("数据格式有误");
        }
        // 网关编码自动转小写
        else {
            if (mf.getAddrStr().equals(mf.getNet())) {
                mf.setAddrStr(mf.getAddrStr().toLowerCase());
            }
            mf.setNet(StrUtil.isNotBlank(mf.getNet()) ? mf.getNet().toLowerCase() : "");
        }

        // 同步网关,并自动上线
        ZaSysDevice zaSysDevice = syncNet(mf, JSONObject.toJSONString(msgObj));

        // 设备地址不等于网关地址 - 尝试同步设备信息
        if (mf.getNet() != null && !mf.getAddrStr().equalsIgnoreCase(mf.getNet())) {
            zaSysDevice = syncFacility(mf, JSONObject.toJSONString(msgObj));
        }
        if (zaSysDevice == null) {
            log.error("注册设备失败 net:{}, addrStr: {}", mf.getNet(), mf.getAddrStr());
            return null;
        }

        //心跳数据，包括巡检
        if (monitorMsg.getEvent().equalsIgnoreCase(MonitorMsg.Event.HEARTBEAT.name())) {
            if (mf.getAnalogValue() == null && mf.getTemperature() == null && mf.getRssi() == null && mf.getVoltage() == null) {
                log.debug("无监测值的心跳数据，暂时忽略");
                return null;
            }
        }

        MessageUtil.setDevice(zaSysDevice);

        // 当前设备编码，如果以”通道“结尾，就去掉通道号
        String fsn = mf.getAddrStr() == null ? null : mf.getAddrStr().trim();

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

        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setCode(fsn);
        facility.setOnLine(true);
        facility.setWireless(mf.isWireless());
        facility.setName(zaSysDevice.getName());
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

        //处理数据
        MqMessage msg = new MqMessage();
        msg.setDeviceId(zaSysDevice.getId());
        msg.setEvent(monitorMsg.getEvent().equalsIgnoreCase("alarm") ? MqMessage.EVENT_ALARM : MqMessage.EVENT_BUSINESS);
        msg.setFacility(facility);
        msg.setMsgData(JSONObject.toJSONString(monitorMsg));
        msg.setTime(new Date());
        msg.setProtocol(PROTOCOL_NAME);
        msg.setUuid(SnowflakeIdWorker.getInstance().nextStringId());

        consumeMsg(msg);

        return ProcessInfo.newInstance(
                zaSysDevice, msg.getMsgData(), msg.getEventType() , Collections.singletonList(msg)
        );
    }

    private void syncAllIfNeeded(ZaSysPlatform platform, int currentPage) {
        if (!Constants.YES.equals(platform.getConfigStr("syncAllDevice", "0"))) {
            log.info("不需要同步青鸟云设备信息!");
            return;
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("pageNum", currentPage);
        params.put("pageSize", 100);
        JSONObject result = JB_API.get(JBApiUtil.AuthType.TOKEN, "/api/facilities/list", params);

        if (result != null && result.getInteger("code") == 1) {
            // 分页参数
            Integer pageNum = result.getInteger("pageNum");
            Integer pageSize = result.getInteger("pageSize");
            Integer total = result.getInteger("total");

            JSONArray data = result.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                // 提取设备信息
                Facility facility = new Facility();
                facility.setId(item.getLong("id"));
                facility.setNet(item.getString("net"));
                facility.setAddrStr(item.getString("addrStr"));
                facility.setFacilitiesCode(item.getInteger("facilitiesCode"));
                facility.setFacilitiesTypeCode(item.getInteger("facilitiesTypeCode"));
                facility.setDescr(item.getString("descr"));
                facility.setIsWireless(item.getString("isWireless"));
                facility.setVoltage(item.getString("voltage"));
                facility.setRssi(item.getString("rssi"));
                facility.setTemperature(item.getString("temperature"));
                facility.setLongitude(item.getBigDecimal("longitude"));
                facility.setLatitude(item.getBigDecimal("latitude"));
                facility.setThresholdHigh(item.getBigDecimal("thresholdHigh"));
                facility.setThresholdLow(item.getBigDecimal("thresholdLow"));
                facility.setAnalogValue(item.getBigDecimal("analogValue"));
                facility.setAnalogType(item.getString("analogType"));
                // 同步设备
                syncFacility(facility, JSON.toJSONString(item));
            }

            // 判断是否为最有一页, 递归执行下一页
            if (pageNum * pageSize < total) {
                log.info("尝试同步下一页数据(当前页:{},总共:{}条数据)", pageNum, total);
                ThreadUtil.sleep(1000);
                syncAllIfNeeded(platform, ++currentPage);
            }
        } else {
            log.error("请求青鸟云设备列表失败:{}", result != null ? result.getString("message") : "响应结果为空");
        }
    }

    /**
     * 同步注册网关设备信息
     *
     * @param mf  the mf
     * @param msg the msg
     * @return za sys device
     */
    private ZaSysDevice syncNet(Facility mf, String msg) {
        String net = mf.getNet();
        if (StringUtils.isEmpty(net)) {
            log.warn("放弃同步网关设备，网关编码为空!");
            return null;
        }
        String typeCode = "128";
        net = net.toLowerCase();
        String name = "网关" + net;
        if (DeviceType.isUITD(net)) {
            typeCode = DeviceType.UITD;
            name = "用传" + net;
        } else {
            typeCode = DeviceType.HRPWLG;
        }
        SyncDevice syncDevice = SyncDevice.builder()
                .code(net)
                .name(name)
                .net(net)
                //.model(mf.getFacilitiesModel())
                .typeCode(typeCode)
                .pfCode(zaSysPlatform.getCode())
                .wireless(typeCode.equals(DeviceType.HRPWLG) ? "1" : "0")
                .build();

       return DeviceUtil.syncDevice(syncDevice , this);
    }

    /**
     * 同步设备信息,不包括网关
     *
     * @param mf  the mf
     * @param msg the msg
     * @return the za sys device
     */
    private ZaSysDevice syncFacility(Facility mf, String msg) {
        // 网关编码
        String net = StrUtil.isBlank(mf.getNet()) ? "" : mf.getNet().trim().toLowerCase();
        // 设备编码
        String code = mf.getAddrStr();
        // 当前设备编码，如果以”通道“结尾，就去掉通道号
        String fsn = mf.getAddrStr() == null ? "" : mf.getAddrStr().trim();

        // 消控主机下属设备
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
                        .typeCode(mf.getFacilitiesTypeCode() == null ? "FAC" : mf.getFacilitiesTypeCode().toString())
                        .model(StrUtil.isBlank(mf.getFacilitiesModel()) ? mf.getModel() : mf.getFacilitiesModel())
                        .pfCode(zaSysPlatform.getCode())
                        .wireless("0")
                        .remark(msg)
                        .build();
                syncDevice.setModel(StrUtil.isBlank(syncDevice.getModel()) ? "未知" : syncDevice.getModel());
                ctlDevice = DeviceUtil.syncDevice(syncDevice ,  this);
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

        // 其消控主机下属设备
        ZaSysDevice componentDevice = deviceService.selectZaSysDeviceByCode(code, net);
        if (componentDevice == null) {
            // 同步设备信息
            SyncDevice syncDevice = SyncDevice.builder()
                    .id(mf.getId())
                    .code(code)
                    .name(name)
                    .net(net)
                    .model(StringUtils.isEmpty(mf.getFacilitiesModel()) ? mf.getModel() : mf.getFacilitiesModel())
                    .typeCode(mf.getFacilitiesTypeCode() + "")
                    .pfCode(zaSysPlatform.getCode())
                    .wireless(mf.isWireless() ? "1" : "0")
                    .remark(msg)
                    .build();
            syncDevice.setModel(StrUtil.isBlank(syncDevice.getModel()) ? "未知" : syncDevice.getModel());
            componentDevice = DeviceUtil.syncDevice(syncDevice , this);

            // TODO
            //pushDevice(componentDevice, MqMessage.DEVICE_ADD, msg);
        }

        return componentDevice;
    }


    /**
     * 获取远程控制token
     *
     * @param zaSysPlatform the za sys platform
     * @return login token
     * @deprecated 青鸟云鉴权改版 ，当前方法已废弃， 新的功能统一使用{@link JBApiUtil}调用青鸟云相关API
     */
    @Deprecated
    private String getLoginToken(ZaSysPlatform zaSysPlatform) {
        Object cacheObj = cache.getCacheObject(JADE_BIRD_TOKEN);
        if (StringUtils.isNotNull(cacheObj)) {
            String tokenCache = StringUtils.cast(cacheObj);
            return tokenCache;
        }
        JSONObject config = JSONObject.parseObject(zaSysPlatform.getConfig());
        ;
        Map<String, String> parammap = new HashMap<>();
        parammap.put("loginName", config.getString("loginName")); // 设施类别，2：现场部件
        parammap.put("password", config.getString("password"));
        String reqParam = getStringOfMap(parammap);
        String res = HttpUtils.sendGet("https://fire-iot.jbufacloud.com/api/security/jwt/tokenOnly", reqParam);
        BaseResponseInfo resInfo = JSONObject.parseObject(res, BaseResponseInfo.class);
        if (resInfo != null && resInfo.isSuccess()) {
            String token = resInfo.getData().getToken();
            cache.setCacheObject(JADE_BIRD_TOKEN, token, 10, TimeUnit.HOURS);
            return token;
        }
        return null;

    }

    /**
     * Gets string of map.
     *
     * @param parammap the parammap
     * @return the string of map
     */
    private String getStringOfMap(Map<String, String> parammap) {
        String paramstr = ""; // name1=value1&name2=value2
        for (Map.Entry<String, String> entry : parammap.entrySet()) {
            String key = entry.getKey();
            paramstr = paramstr + key + "=" + entry.getValue() + "&";
        }
        return paramstr.substring(0, paramstr.length() - 1);
    }
}
