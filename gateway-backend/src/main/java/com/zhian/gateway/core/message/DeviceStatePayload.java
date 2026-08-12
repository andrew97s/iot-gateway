package com.zhian.gateway.core.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备状态负载
 *
 * @author tongwenjin
 * @since 2026/7/30
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceStatePayload extends MessagePayload {

    /**
     * 是否在线: 1 在线, 0 离线
     */
    private String online;

    /**
     * 状态变化原因
     */
    private String reason;
}
