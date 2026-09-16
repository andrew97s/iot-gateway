package com.zhian.gateway.sys.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标准监测类型 za_monitor_type
 *
 * 设备实时监测项字典，分两类：
 * 1. enum   枚举值：在线/离线、开关量等，取值范围由 enumOptions 定义
 * 2. linear 线性值：压力、液位、液压、电流、电压、信号强度、电量等连续变化值，配合 unit 单位
 */
@ApiModel(value = "ZaMonitorType", description = "标准监测类型")
@Data
@TableName("za_monitor_type")
public class ZaMonitorType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 值类型：枚举 */
    public static final String VALUE_TYPE_ENUM = "enum";
    /** 值类型：线性 */
    public static final String VALUE_TYPE_LINEAR = "linear";

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键")
    private Long id;

    /** 类型编码 */
    @ApiModelProperty("类型编码")
    private String code;

    /** 类型名称 */
    @ApiModelProperty("类型名称")
    private String name;

    /** 值类型：enum 枚举 / linear 线性 */
    @ApiModelProperty("值类型：enum/linear")
    private String valueType;

    /** 监测单位（线性值使用，如 MPa、V、A、%、dBm） */
    @ApiModelProperty("监测单位")
    private String unit;

    /**
     * 枚举值定义 JSON 数组（valueType=enum 时使用）：
     * [{"value":"1","label":"在线"},{"value":"0","label":"离线"}]
     */
    @ApiModelProperty("枚举值定义(JSON)")
    private String enumOptions;

    /**
     * 插件别名映射 JSON 数组：[{"pfCode":"jb","alias":"rssi"}]
     */
    @ApiModelProperty("插件别名映射(JSON)")
    private String aliases;

    /** 状态：1启用 0停用 */
    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 查询参数（非表字段） */
    @TableField(exist = false)
    private String pfCode;
}
