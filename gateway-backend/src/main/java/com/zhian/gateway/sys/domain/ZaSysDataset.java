package com.zhian.gateway.sys.domain;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据集对象 za_sys_dataset
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@ApiModel(value = "ZaSysDataset", description = "数据集")
@Data
public class ZaSysDataset extends BaseEntity
{
    public static final String TYPE_COUNT = "1";
    public static final String TYPE_STATICS = "2";
    public static final String TYPE_QUERY = "3";
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 类型 */
    @Excel(name = "类型")
    @ApiModelProperty("类型")
    private String type;

    /** 名称 */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /** 代码 */
    @Excel(name = "代码")
    @ApiModelProperty("代码")
    private String code;

    /** 查询语句 */
    @ApiModelProperty("代码")
    private String sqls;

    /** 参数 */
    @Excel(name = "参数")
    @ApiModelProperty("参数")
    private String param;



}
