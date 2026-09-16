package com.zhian.gateway.third.video.vo;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.TreeEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 设备类型对象 za_facility_type
 * 
 * @author yepanpan
 * @date 2024-04-10
 */
@ApiModel(value = "ZaFacilityType", description = "设备类型")
@Data
public class ZaFacilityType extends TreeEntity
{
    public static final String TECH_CONTROLLER = "1";
    public static final String TECH_COMPONENT = "2";
    public static final String TECH_NET = "3";
    public static final String TECH_IOT = "4";

    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 上级分类 */
    @Excel(name = "上级分类")
    private String parentName;

    /** 类型代码 */
    @Excel(name = "类型代码")
    @ApiModelProperty("类型代码")
    private String code;

    /** 名称 */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /** 简称 */
    @Excel(name = "简称")
    @ApiModelProperty("简称")
    private String shortName;

    /** 子系统 */
    @Excel(name = "子系统")
    private String sysTypeName;
    private String sysTypeCode;
    @ApiModelProperty("子系统")
    private Long sysTypeId;

    /** 是否为叶子节点（Y是 N否） */
    @Excel(name = "是否为叶子节点", dictType = "sys_yes_no", readConverterExp = "Y=是,N=否")
    @ApiModelProperty("是否为叶子节点")
    private String isLeaf;


    /** 技术分类 */
    @Excel(name = "技术分类", dictType = "facility_tech_type")
    @ApiModelProperty("技术分类")
    private String techType;

    /** 青鸟类型 */
    @ApiModelProperty("是否为现场部件节点")
    private String jbCode;

}
