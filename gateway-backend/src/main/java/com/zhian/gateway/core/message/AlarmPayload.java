package com.zhian.gateway.core.message;

import com.zhian.gateway.sys.domain.ZaAlarmType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息负载-告警
 *
 * @author tongwenjin
 * @since 2026/7/20
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AlarmPayload extends  MessagePayload {

    /**
     * 告警类型对应 {@link ZaAlarmType#getCode()}
     */
    private String code;

    /**
     * 告警类型名称对应 {@link ZaAlarmType#getName()}
     */
    private String name;

    /**
     * 告警状态： 产生 active 、 恢复 recovered
     */
    private String state;

    /**
     * 告警描述
     */
    private String desc;

    /**
     * 告警时间戳
     */
    private long timestamp;
}
