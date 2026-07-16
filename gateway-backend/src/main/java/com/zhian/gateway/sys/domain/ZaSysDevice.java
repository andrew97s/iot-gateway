package com.zhian.gateway.sys.domain;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 接入设备对象 za_sys_device
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
@ApiModel(value = "ZaSysDevice", description = "接入设备")
@Data
public class ZaSysDevice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    @ApiModelProperty("业务ID")
    private String bizId;

    /** 设备类别 */
    @Excel(name = "设备类别")
    @ApiModelProperty("设备类别")
    @NotNull
    private String type;

    /** 设备型号 */
    @Excel(name = "设备型号")
    @ApiModelProperty("设备型号")
    private String model;

    /** 网关代码 */
    @Excel(name = "网关代码")
    @ApiModelProperty("网关代码")
    private String net;

    /** 设备编码 */
    @Excel(name = "设备编码")
    @ApiModelProperty("设备编码")
    @NotNull
    private String code;

    /** 设备位置 */
    @Excel(name = "设备位置")
    @ApiModelProperty("设备位置")
    @NotNull
    private String name;

    /** 经度 */
    @Excel(name = "经度")
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    /** 无线设备 */
    @Excel(name = "无线设备", dictType = "sys_yes_no")
    @ApiModelProperty("无线设备")
    private String wireless;

    /** 归属平台 */
    @Excel(name = "归属平台")
    @ApiModelProperty("归属平台")
    private String pfCode;

    /** 级联平台 */
    //@Excel(name = "级联平台")
    @ApiModelProperty("级联平台")
    private Long cascadeId;

    /** 是否在线 */
    @Excel(name = "是否在线")
    @ApiModelProperty("是否在线：1在线，0离线")
    private String online;

    /** IP */
    @Excel(name = "IP")
    @ApiModelProperty("IP")
    private String ip;

    /** 上级同步展示（非表字段） */
    @ApiModelProperty(hidden = true)
    private Integer syncSuccess;
    @ApiModelProperty(hidden = true)
    private Integer syncTotal;
    @ApiModelProperty(hidden = true)
    private String syncLabel;
}
