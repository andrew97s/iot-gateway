package com.zhian.gateway.third.others.cr;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.utils.SerialPortUtil;
import com.zhian.gateway.third.video.roc_v2.FmConnector;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 读卡器（Card Reader）处理器
 *
 * @author tongwenjin
 * @since 2025-3-17
 */
@Slf4j
@Component
public class CrHandler extends BasePlatformHandler<Object> {

    private CrConnector connector = new CrConnector();

    @Override
    public boolean start(ZaSysPlatform platform) {
        syncModuleDevice();
        return connector.connectPort(SerialPortUtil.searchCrDevice());
    }

    @Override
    public boolean stop() {
        // 停止CR进程
        connector.closePort();
        return true;
    }

    @Override
    public boolean isAlive() {
        // 校验串口供电是否正常,长时间供电需要主动切断供电
        return connector != null  && connector.isOpen();
    }

    @Override
    public String getPlatform() {
        return "cr_v1";
    }

    @Override
    public String getProtocol() {
        return "cr";
    }

    @Override
    public R doControl(ControlVo controlVo) {
        // 读取卡片ID
        if (ControlVo.CMD_QRY.equals(controlVo.getCommand())) {
            SerialPortUtil.powerOnNoDelay();
            String code = connector.readCardCode();
            return StrUtil.isNotBlank(code) ?
                    R.success("OK",code) : R.error("识别为空");
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
            throw new UnsupportedOperationException("操作失败,反控指令: " + controlVo.getCommand() + " 不支持!");
        }
    }

    private void syncModuleDevice() {
        String sn = "cr_v1_001";
        String platformName = getPlatform();

        ZaSysDevice sysDevice = deviceService.selectByCode(sn, platformName);
        if (sysDevice == null) {
            sysDevice = new ZaSysDevice();
            sysDevice.setType(platformName);
            sysDevice.setOnline("1");
            sysDevice.setIp(NetUtil.getLocalhostStr());
            sysDevice.setCode(sn);
            // sysDevice.setNet(platformName);
            sysDevice.setName("读卡器模块(" + sn + ")");
            sysDevice.setPfCode(platformName);
            sysDevice.setWireless("0");
            sysDevice.setModel("cr_v1");
            deviceService.insertZaSysDevice(sysDevice);
        }

        //推送设备信息
        MqMessage.Facility facility = new MqMessage.Facility();
        facility.setType(sysDevice.getType());
        facility.setPfCode(getPlatform());
        facility.setCode(sysDevice.getCode());
        facility.setName(sysDevice.getName());
        facility.setModel(sysDevice.getModel());
        facility.setNet(sysDevice.getNet());
        facility.setWireless(false);
        facility.setOnLine(true);
        MqMessage message = MqMessage.createDevice(
                sysDevice.getId(), getProtocol(), facility, sysDevice.getRemark()
        );
        message.setEventType(MqMessage.DEVICE_ADD);
        log.info("同步读卡器模块设备信息至消安...");
        consumeMsg(message);
    }
}
