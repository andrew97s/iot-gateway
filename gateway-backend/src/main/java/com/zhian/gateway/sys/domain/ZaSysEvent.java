package com.zhian.gateway.sys.domain;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 告警事件对象 za_sys_event
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@ApiModel(value = "ZaSysEvent", description = "告警事件")
@Data
public class ZaSysEvent extends BaseEntity
{
    /** 复位 **/
    public static final String JB_RESET = "33";

    /** 告警产生 **/
    public static final String ALARM_CREATE = "1";

    /** 告警撤销 **/
    public static final String ALARM_REPEAL = "2";

    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 告警大类 */
    private String bigType;

    /** 告警类型 */
    @Excel(name = "告警类型")
    @ApiModelProperty("告警类型")
    private String alarmType;

    /** 原始代码 */
    @Excel(name = "原始代码")
    @ApiModelProperty("原始代码")
    private String sourceCode;

    /** 事件厂商 */
    @Excel(name = "事件厂商")
    @ApiModelProperty("事件厂商")
    private String vendorCode;

    /** 事件描述 */
    @Excel(name = "事件描述")
    @ApiModelProperty("事件描述")
    private String comment;

    /** WS操作 */
    @Excel(name = "WS操作")
    @ApiModelProperty("WS操作")
    private String wsOption;

    /** 告警状态 */
    @Excel(name = "告警状态")
    @ApiModelProperty("告警状态")
    private String alarmState;

    /** 开关状态 */
    @Excel(name = "开关状态")
    @ApiModelProperty("开关状态")
    private String switchState;


}
