package com.zhian.gateway.third.video.vo;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设备型号对象 za_facility_model
 * 
 * @author yepanpan
 * @date 2024-04-10
 */
@ApiModel(value = "ZaFacilityModel", description = "设备型号")
@Data
public class ZaFacilityModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 设备类型ID */
    @Excel(name = "设备类型")
    private String typeName;
    @ApiModelProperty("设备类型ID")
    private Long typeId;

    /** 名称 */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /** 通讯协议，有线、4G、NB、lora */
    @Excel(name = "通讯协议", dictType = "net_protocol")
    @ApiModelProperty("通讯协议，有线、4G、NB、lora")
    private String protocolType;

    /** 厂商 */
    @Excel(name = "厂商")
    @ApiModelProperty("厂商")
    private String manufacturer;

    /** 控制面板 */
    @Excel(name = "控制面板", dictType = "sys_yes_no")
    @ApiModelProperty("控制面板")
    private String controlPanel;

    /** 反控能力，多个用逗号分隔 */
    @Excel(name = "反控能力，多个用逗号分隔")
    @ApiModelProperty("反控能力，多个用逗号分隔")
    private String ability;

}
