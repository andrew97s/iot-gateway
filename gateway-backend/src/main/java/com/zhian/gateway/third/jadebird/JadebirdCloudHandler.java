package com.zhian.gateway.third.jadebird;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.core.message.Message;
import com.zhian.gateway.core.message.MsgProcessContext;
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
import com.zhian.gateway.third.jadebird.util.JbProtocolParser;
import com.zhian.gateway.third.jadebird.vo.BaseResponseInfo;
import com.zhian.gateway.third.jadebird.vo.Facility;
import com.zhian.gateway.third.jadebird.vo.MonitorMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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
     * The constant JB_API.
     */
    private static JBApiUtil JB_API;
    @Autowired
    private IZaSysDeviceService deviceService;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        super.start(zaSysPlatform);
        JB_API = new JBApiUtil(zaSysPlatform);
        log.info("等待青鸟云平台推送过来的数据");
        // 尝试同步平台设备 TODO 同步设备状态
        CompletableFuture.runAsync(() -> syncAllIfNeeded(zaSysPlatform, 1)).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
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
    public R doControl(ControlVo controlVo) {
        // 校验
        if (!isAlive()) {
            log.error("青鸟云插件暂时停止");
            return R.error("插件暂时停止");
        }
        ZaSysDevice device = controlVo.getDevice();
        device = deviceService.selectZaSysDeviceByCode(device.getCode(), device.getNet());
        if (device == null) {
            return R.error("反控操作失败,device 不能为空!");
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
        return R.success();
    }

    /**
     * 处理接收到的消息 TODO
     *
     * @param msgObj
     * @return
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

    private void syncAllIfNeeded(ZaSysPlatform platform, int currentPage) {
        if (!StrUtil.equals(platform.getConfigStr("syncAllDevice", "false") , "true")) {
            log.info("不需要同步青鸟云设备信息!");
            return;
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("pageNum", currentPage);
        params.put("pageSize", 1000);
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
                this.manualSyncDevice(
                        ()-> JbProtocolParser.requireDevice(facility, getPlatform()) ,
                        JSON.toJSONString(facility)
                );
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
}
