package com.zhian.gateway.third.jadebird;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.DictValue;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.TelemetryPayload;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.*;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.AlarmType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.jadebird.util.JBV3ApiUtil;
import com.zhian.gateway.third.jadebird.vo.*;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 青鸟云平台对接处理器V3
 *
 * 对接文档见<a href="https://jbufa-v3.apifox.cn/">对接文档</a>
 */
@Component
@Slf4j
public class JadebirdCloudV3Handler extends BasePlatformHandler {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "jbV3";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "jbV3";
    /**
     * The constant zaSysPlatform.
     */
    private static ZaSysPlatform zaSysPlatform;
    /**
     * The constant running.
     */
    private static Boolean running = false;

    @Autowired
    private JBV3ApiUtil jbv3ApiUtil;
    @Autowired
    private Cache cache;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        JadebirdCloudV3Handler.zaSysPlatform = zaSysPlatform;
        jbv3ApiUtil.start(zaSysPlatform);
        log.info("青鸟云V3服务启动成功");
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        log.info("青鸟云V3服务停止完成");
        running = false;
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     *
     * @return
     */
    @Override
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
    public R control(ControlVo controlVo) {
        // 校验
        if (!isAlive()) {
            log.error("青鸟云插件暂时停止");
            return R.error("插件暂时停止");
        }
        String code = controlVo.getCode();
        if (StringUtils.isEmpty(code)) {
            return R.error("反控操作失败,code 不能为空!");
        }
        String[] cmds = controlVo.getCommand().split(",");
        for (String cmd : cmds) {
            // 消音
            if (ControlVo.CMD_MUTE.equals(cmd)) {
                jbv3ApiUtil.mute(code, 3);
            } else if (ControlVo.CMD_RESET.equals(cmd)) {
                // 复位 - 待实现
                log.warn("反控操作暂未实现复位操作!");
            }
        }
        return R.success();
    }

    /**
     * 处理接收到的消息
     *
     * @param msgObj
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        JadeBirdMsgV3 monitorMsg = (msgObj instanceof JadeBirdMsgV3) ?
                (JadeBirdMsgV3) msgObj : JSONObject.parseObject(msgObj.toString(), JadeBirdMsgV3.class);
        JaderBirdFacilityV3 mf = monitorMsg.getFacility();
        if (mf == null || StringUtils.isEmpty(mf.getAddr())) {
            throw new ServiceException("数据格式有误");
        }
        // 同步网关
        ZaSysDevice sysDevice = syncFacility(mf, JSONObject.toJSONString(msgObj));
        // 当前设备编码，如果以”通道“结尾，就去掉通道号
        String fsn = mf.getAddr() == null ? "" : mf.getAddr().trim();
        Integer chn = null;
        if (fsn.endsWith("通道")) {
            chn = Integer.parseInt(fsn.substring(fsn.indexOf(' ') + 1).substring(0, 1));
        }
        // 剔除 【通道 、 线路】 关键字
        if (fsn.contains("通道") || fsn.contains("线路")) {
            fsn = fsn.substring(0, fsn.lastIndexOf(' ')).trim();
        }
        MessageUtil.setDevice(sysDevice);
        MsgProcessContext.getProcessInfo().setDevice(sysDevice);
        Map<String, String> dictModelMap = jbv3ApiUtil.getDict(JBV3ApiUtil.DictionaryType.FACILITY_MODEL.getValue(), 3);
        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setCode(fsn);
        facility.setInOnline(true);
        facility.setWireless(true);
        facility.setName(sysDevice.getName());
        facility.setNet("");
        facility.setChn(chn);
        facility.setModel(dictModelMap.get(mf.getModelCode() + ""));
        facility.setType(mf.getTypeCode() + "");
        //模拟量
        if (StringUtils.isNotEmpty(mf.getMeter())) {
            Map<String, String> dictMeterUnitMap = jbv3ApiUtil.getDict(
                    JBV3ApiUtil.DictionaryType.METER_UNIT.getValue(), 2
            );
            //传感器类型
            mf.getMeter().forEach(v -> {
                v.setMeterUnitName(dictMeterUnitMap.get(v.getMeterUnitCode() + ""));
                if (v.getSensorCode() == 10) {
                    //电量
                    facility.setVoltage(v.getMeterValue().intValue());
                } else if (v.getSensorCode() == 1) {
                    //温度
                    facility.setTemperature(v.getMeterValue().intValue());
                } else if (v.getSensorCode() == 11) {
                    //信号
                    facility.setRssi(v.getMeterValue().intValue());
                }
            });

            // TODO 记录设备实时监测数据
        }


        // 业务监测数据
        String currentValue = "";
        if (Objects.nonNull(monitorMsg.getFacility()) && CollUtil.isNotEmpty(monitorMsg.getFacility().getMeter())) {
            List<TelemetryPayload.Telemetry> telemetries = new ArrayList<>();
            for (MeterV3 meter : monitorMsg.getFacility().getMeter()) {
                // 监测名称
                String desc = "";
                if (meter.getSensorCode() != null) {
                    desc = cache.getCacheMap("jbf_v3_dict:sensor").get(meter.getSensorCode() + "").toString();
                }

                // 监测数据
                TelemetryPayload.Telemetry telemetry = MessageBuilder.builderTelemetry(
                        fetchMonitorType(meter.getMeterUnitCode() + ""), meter.getMeterValue().toString(), desc
                );

                if (meter.getSensorCode() == 3 || meter.getSensorCode() == 4) {
                    currentValue = meter.getMeterValue() + " " + meter.getMeterUnitName();
                }

                telemetries.add(telemetry);
            }

            if (!telemetries.isEmpty()) {
                MsgProcessContext.addMsg(MessageBuilder.buildTelemetry(sysDevice, telemetries));
            }
        }

        // 事件
        List<JaderBirdEventV3> eventList = monitorMsg.getEventList();
        if (CollUtil.isNotEmpty(eventList)) {
            Map<String, String> dictStateMap = jbv3ApiUtil.getDict(JBV3ApiUtil.DictionaryType.STATE.getValue(), 2);
            eventList.forEach(v -> v.setStateName(dictStateMap.get(v.getStateCode() + "")));
            // 告警数据
            for (JaderBirdEventV3 event : eventList) {
                TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
                Optional<ZaAlarmType> type = typeMapping.resolveAlarmType(getPlatform(), event.getStateCode() + "");
                if (!type.isPresent()) {
                    log.info("提取青鸟告警事件类型失败:{}未注册告警类型!", event.getStateCode());
                    continue;
                }

                // 阈值告警
                String desc = event.getStateName();
                if (event.getStateCode() == 46 || event.getStateCode() == 230) {
                    desc += "(" + currentValue + ")";
                }

                MsgProcessContext.addMsg(
                        MessageBuilder.buildAlarm(sysDevice, type.get(), desc, "")
                );
            }
        }

        return null;
    }

    private ZaMonitorType fetchMonitorType(String code) {
        TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
        Optional<ZaMonitorType> type = typeMapping.resolveMonitorType(getPlatform(), code);
        if (!type.isPresent()) {
            log.error("获取青鸟监测值类型失败:{}未注册!" , code);

            return null;
        }

        return type.get();
    }


    /**
     * 同步设备信息,不包括网关
     *
     * @param mf  the mf
     * @param msg the msg
     * @return the za sys device
     */
    private ZaSysDevice syncFacility(JaderBirdFacilityV3 mf, String msg) {
        // 设备编码
        String code = mf.getAddr();
        //同步部件信息
        if (code.contains("通道") || code.contains("线路")) {
            code = code.substring(0, code.lastIndexOf(' ')).trim();
        }
        String name = StringUtils.isNotEmpty(mf.getDescr()) ? mf.getDescr() : code;
        // 其消控主机下属设备
        ZaSysDevice componentDevice = deviceService.selectZaSysDeviceByCode(code, null);
        if (componentDevice == null) {
            // 解析设备类型
            TypeMappingService typeMapping = SpringUtils.getBean(TypeMappingService.class);
            Optional<ZaDeviceType> mfType = typeMapping.resolveDeviceType(getPlatform(), mf.getTypeCode() + "");
            String typeCode = "currencyComponent";
            if (mfType.isPresent()) {
                typeCode = mfType.get().getCode();
            }
            Map<String, String> dictMap = jbv3ApiUtil.getDict(JBV3ApiUtil.DictionaryType.FACILITY_MODEL.getValue(), 3);
            // 同步设备信息
            SyncDevice syncDevice = SyncDevice.builder()
                    .id(mf.getId())
                    .code(code)
                    .name(name)
                    .net(null)
                    .model(dictMap.get(mf.getModelCode() + ""))
                    .typeCode(typeCode)
                    .pfCode(zaSysPlatform.getCode())
                    .wireless("1")
                    .build();
            componentDevice = DeviceUtil.syncDevice(syncDevice);
        }

        return componentDevice;
    }


}
