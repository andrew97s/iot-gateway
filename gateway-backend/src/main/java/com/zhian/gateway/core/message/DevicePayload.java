package com.zhian.gateway.core.message;

import com.zhian.gateway.sys.domain.ZaDeviceType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 负载-设备消息
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DevicePayload extends MessagePayload {

    /**
     * 设备消息类型: add delete update
     */
    private String action;

    /**
     * 设备ID - 网关内唯一{@link ZaDeviceType#getId()}
     */
    private long deviceId;

    /**
     * 设备编码{@link ZaDeviceType#getCode()}
     */
    private String code;

    /**
     * 设备名称{@link ZaDeviceType#getName()}
     */
    private String name;

    /**
     * 设备类型{@link ZaDeviceType#getCode()}
     */
    private String typeCode;

    /**
     * 设备网关{@link ZaSysDevice#getNet()}
     */
    private String net;

    /**
     * 设备安装位置{@link ZaSysDevice#getLocation}
     */
    private String position;

    /**
     * 设备最新状态 - 1 在线 、 0 离线
     */
    private String online;

    /**
     * 执行原因
     */
    private String reason;
}
