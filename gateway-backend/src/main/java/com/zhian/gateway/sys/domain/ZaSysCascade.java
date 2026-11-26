package com.zhian.gateway.sys.domain;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 级联平台对象 za_sys_cascade
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
@ApiModel(value = "ZaSysCascade", description = "级联平台")
@Data
public class ZaSysCascade extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 名称 */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /** 代码 */
    @Excel(name = "代码")
    @ApiModelProperty("代码")
    private String code;

    /** IP */
    @Excel(name = "IP")
    @ApiModelProperty("IP")
    private String ip;

    /** 状态 */
    @Excel(name = "状态")
    @ApiModelProperty("状态")
    private String status;

    /** 是否在线 */
    @Excel(name = "是否在线")
    @ApiModelProperty("是否在线")
    private String online;



}
