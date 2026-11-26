package com.zhian.gateway.third.cascade;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.consts.CascadeConst;
import com.zhian.gateway.sys.domain.ZaSysCascade;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysCascadeService;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 级联上级服务，处理接收到的级联消息
 * 1、如果是cascade级联消息，认为是反控结果，保存到缓存
 * 2、其它消息转发到RabbitMQ
 */
@Component
@Slf4j
public class CascadeServerHandler extends BasePlatformHandler {

    @Autowired
    private IZaSysCascadeService zaSysCascadeService;

    @Override
    public String getPlatform() {
        return CascadeConst.PLATFORM_CODE;
    }

    @Override
    public String getProtocol() {
        return CascadeConst.PLATFORM_CODE;
    }

    /**
     * 向下发送反控指令
     *
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceById(controlVo.getDeviceId());
        ZaSysCascade zaSysCascade = zaSysCascadeService.selectZaSysCascadeById(zaSysDevice.getCascadeId());
        CascadeServerSocket.sendTo(zaSysCascade.getCode(), JSONObject.toJSONString(controlVo));
        return R.success();
    }

    /**
     * 级联上级服务端接收到的是下级上报的数据，包括发现的设备、心中、告警和反向控制处理结果
     *
     * @param msgObj
     */
    @Override
    public void processMsg(Object msgObj) {
        MqMessage mqMessage = null;
        if (msgObj instanceof MqMessage) {
            mqMessage = (MqMessage) msgObj;
        } else {
            mqMessage = JSONObject.parseObject(msgObj.toString(), MqMessage.class);
        }

        if (mqMessage == null) {
            log.error("不支持的消息格式： {}", msgObj);
        }

        if (mqMessage.getProtocol().equalsIgnoreCase("cascade")) {
            //保存下级响应
            cache.setCacheMapValue(CascadeConst.CACHE_MAP_KEY, mqMessage.getUuid(), mqMessage.getMsgData());
        } else if (mqMessage.getDeviceId() != null) {
            // 转发数据到本级的RabbitMQ
            SpringUtils.getBean(MessageSyncHandler.class).processMsg(mqMessage);
        }
    }

    /**
     * 同步注册下级的设备信息
     *
     * @param mf
     * @param cascadeId
     * @return
     */
    public ZaSysDevice syncCascadeDevice(MqMessage.Facility mf, Long deviceId, Long cascadeId) {

        ZaSysDevice old = deviceService.selectZaSysDeviceById(deviceId);
        if (old == null) {
            ZaSysDevice dc = new ZaSysDevice();
            dc.setCode(mf.getCode());
            if (StringUtils.isNotEmpty(mf.getNet())) {
                dc.setNet(mf.getNet());
            }

            dc.setName(mf.getName());
            dc.setModel(mf.getModel());
            dc.setType(mf.getType());
            dc.setPfCode(mf.getPfCode());
            dc.setCascadeId(cascadeId);
            dc.setId(deviceId);
            deviceService.insertZaSysDevice(dc);
            return dc;
        } else if (old.getCascadeId() == null || !old.getCascadeId().equals(cascadeId)) {
            old.setCascadeId(cascadeId);
            deviceService.updateZaSysDevice(old);
        }
        return old;
    }

}
