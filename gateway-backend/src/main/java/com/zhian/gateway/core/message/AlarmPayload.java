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
     * 告警类别对应 {@link ZaAlarmType#getType()}
     */
    private Integer type;

    /**
     * 告警类型对应 {@link ZaAlarmType#getCode()}
     */
    private String code;

    /**
     * 告警类型名称对应 {@link ZaAlarmType#getName()}
     */
    private String name;

    /** 告警产生 */
    public static final String STATE_ACTIVE = "active";
    /** 告警恢复 */
    public static final String STATE_RECOVERED = "recovered";

    /**
     * 告警状态： 产生 {@link #STATE_ACTIVE} 、 恢复 {@link #STATE_RECOVERED}
     */
    private String state;

    /**
     * 告警描述
     */
    private String desc;

    /**
     * 告警图片, 可能为多个使用, 分隔
     */
    private String picUrl;

    /**
     * 告警时间戳
     */
    private long timestamp;
}
