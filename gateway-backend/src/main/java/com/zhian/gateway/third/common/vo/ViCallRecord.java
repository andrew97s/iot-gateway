package com.zhian.gateway.third.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 可视化对讲对象 za_safe_call_record
 * 
 * @author zhian
 * @date 2025-06-04
 */
@Data
public class ViCallRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息代码 */
    private String msgId;

    /** 通话时长 */
    private Long callDuration;

    /** 被叫设备名称 */
    private String calledDeviceName;

    /** 被叫设备编码 */
    private String calleeCode;

    /** 被叫呼叫号码 */
    private String calleeSipId;

    /** 主叫设备编码 */
    private String callerCode;

    /** 主叫呼叫号码 */
    private String callerSipId;

    /** 主叫设备名称 */
    private String callingDeviceName;

    /** 接通状态 */
    private String connectStatus;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

}
