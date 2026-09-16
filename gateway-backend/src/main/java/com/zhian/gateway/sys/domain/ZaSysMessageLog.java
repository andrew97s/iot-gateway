package com.zhian.gateway.sys.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息推送记录 za_sys_message_push_log
 */
@ApiModel(value = "ZaSysMessageLog", description = "消息推送记录")
@Data
public class ZaSysMessageLog implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILURE = "failure";

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("消息主键")
    private Long messageId;

    /** mq / url / redis */
    @ApiModelProperty("推送方式")
    private String type;

    @ApiModelProperty("接收地址或目标摘要")
    private String target;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("推送时间")
    private Date time;

    /** success / failure */
    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("失败原因")
    private String failReason;
}
