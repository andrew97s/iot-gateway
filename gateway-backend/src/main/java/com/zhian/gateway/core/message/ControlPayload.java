package com.zhian.gateway.core.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备反控消息负载
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ControlPayload extends MessagePayload {

    /**
     * 设备反控指令
     */
    private String cmd;

    /**
     * 设备反控参数
     */
    private String params;
}
