package com.zhian.gateway.third.jadebird;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.DeviceTypeEnum;
import com.zhian.gateway.core.message.MsgProcessContext;
import com.zhian.gateway.core.message.builder.MessageBuilder;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.TypeMappingService;
import com.zhian.gateway.sys.utils.MessageUtil;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.bo.SyncDevice;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.jadebird.vo.BoxMsg;
import com.zhian.gateway.third.jadebird.vo.BoxV2AlarmInfo;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 青鸟云盒(特殊项目临时使用)对接
 */
@Component
@Slf4j
public class JadebirdBoxV2Handler extends BasePlatformHandler<BoxMsg> {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "jbox_v2";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "jbox_v2";
    /**
     * The constant CACHE_MAP.
     */
    public static final String CACHE_MAP = "jbox_v2";
    /**
     * The constant OFFLINE_HOURS.
     */
    public static final Integer OFFLINE_HOURS = 1;
    /**
     * The constant zaSysPlatform.
     */
    private static ZaSysPlatform zaSysPlatform;
    /**
     * The constant running.
     */
    private static Boolean running = false;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        JadebirdBoxV2Handler.zaSysPlatform = zaSysPlatform;
        log.info("等待青鸟云盒-V2推送过来的数据");
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        log.info("将忽略青鸟云盒-V2推送过来的数据");
        running = false;
        return true;
    }

    @Override
    public boolean isAlive() {
//        ZaSysDevice dc = new ZaSysDevice();
//        dc.setOnline(DictValue.DEVICE_ONLINE);
//        dc.setPfCode(getPlatform());
//        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
//
//        for (ZaSysDevice zaSysDevice : list) {
//            Long last = cache.getCacheMapValue(CACHE_MAP, "last_" + zaSysDevice.getId());
//            //判断是否离线
//            if (zaSysDevice.getOnline().equalsIgnoreCase(DictValue.DEVICE_ONLINE)
//                    && (last == null || last + OFFLINE_HOURS * 3600 * 1000 < System.currentTimeMillis())) {
//
//                zaSysDevice.setOnline(DictValue.DEVICE_OFFLINE);
//                deviceService.updateZaSysDevice(zaSysDevice);
//
//                // TODO 离线消息
//                // pushState(zaSysDevice, AlarmType.OFFLINE.getCode());
//            }
//        }
        return running;
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return "jbox";
    }

    @Override
    public R doControl(ControlVo controlVo) {
        log.info("青鸟云盒-V2暂不支持反控");
        return R.error(400, "青鸟云盒暂不支持反控");
    }

    @Override
    public ProcessInfo doProcessMsg(BoxMsg boxMsg) {
        String msg = boxMsg.getMsg();
        // 心跳
        if (boxMsg.getType().equalsIgnoreCase(BoxMsg.TYPE_HEARTBEAT)) {
            extractSysDevice(boxMsg);
        }
        // 告警
        else if (boxMsg.getType().equalsIgnoreCase(BoxMsg.TYPE_ALARM)) {
            ZaSysDevice sysDevice = extractSysDevice(boxMsg);
            TypeMappingService mappingService = SpringUtils.getBean(TypeMappingService.class);

            BoxV2AlarmInfo boxInfo = JSONObject.parseObject(msg, BoxV2AlarmInfo.class);
            for (BoxV2AlarmInfo.AlarmInfo alarmInfo : boxInfo.getAlarmArray()) {

                Optional<ZaAlarmType> type = mappingService.resolveAlarmType(getPlatform(), alarmInfo.getClassid());

                if (!type.isPresent()) {
                    log.error("云盒告警处理失败,告警类型:{}未注册!" , alarmInfo.getClassid());
                    continue;
                }

                MsgProcessContext.addMsg(
                        MessageBuilder.buildAlarm(
                                sysDevice, type.get() , type.get().getName() , boxInfo.getAlarmPicName()
                        )
                );
            }



            MessageUtil.setDevice(sysDevice);
            return null;
        } else {
            log.error("不支持云盒消息类型: {}", boxMsg.getType());
        }

        return null;
    }

    /**
     * 从盒子消息中提取系统设备信息
     *
     * @param boxMsg 盒子消息
     * @return 注册系统设备
     */
    private ZaSysDevice extractSysDevice(BoxMsg boxMsg) {
        BoxV2AlarmInfo boxInfo = JSONObject.parseObject(boxMsg.getMsg(), BoxV2AlarmInfo.class);

        // 同步云盒设备信息
        SyncDevice syncDevice = SyncDevice.builder()
                .code(StrUtil.isNotBlank(boxInfo.getSerialNo()) ? boxInfo.getSerialNo() : boxInfo.getMachineCode())
                .name("云盒V2-" + (boxInfo.getIp()))
                .model("JBF-ZA-B1")
                .typeCode(DeviceTypeEnum.WCB.getCode())
                .pfCode(zaSysPlatform.getCode())
                .wireless("0")
                .ip(boxInfo.getIp())
                .remark(JSONObject.toJSONString(boxInfo)).build();

        ZaSysDevice zaSysDevice = DeviceUtil.syncDevice(syncDevice);

        MessageUtil.setDevice(zaSysDevice);

        return zaSysDevice;
    }

    /**
     * 心跳逻辑
     *
     * @param boxMsg the msg
     */
    private MqMessage processHeart(BoxMsg boxMsg) {
        ZaSysDevice zaSysDevice = extractSysDevice(boxMsg);

        return null;
    }
}
