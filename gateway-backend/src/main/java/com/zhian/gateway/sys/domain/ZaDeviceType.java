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
 * 标准设备类型 za_device_type
 *
 * 网关的标准设备类型字典。不同接入插件上报的厂商设备类型（别名）
 * 通过 aliases 映射到标准设备类型；多个插件别名可对应同一个设备类型。
 */
@ApiModel(value = "ZaDeviceType", description = "标准设备类型")
@Data
@TableName("za_device_type")
public class ZaDeviceType implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键")
    private Long id;

    /** 类型编码 */
    @ApiModelProperty("类型编码")
    private String code;

    /** 类型名称 */
    @ApiModelProperty("类型名称")
    private String name;

    /**
     * 插件别名映射 JSON 数组：[{"pfCode":"jb","alias":"11"},{"pfCode":"dhsdk","alias":"smoke"}]
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
