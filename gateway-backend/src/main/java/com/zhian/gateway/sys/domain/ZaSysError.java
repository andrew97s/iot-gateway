package com.zhian.gateway.sys.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 错误日志对象 za_sys_error
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@ApiModel(value = "ZaSysError", description = "错误日志")
@Data
public class ZaSysError extends BaseEntity
{
    /** 接口超时 **/
    public static final String TYPE_API_TIMEOUT = "1";
    /** 接口错误 **/
    public static final String TYPE_API_ERROR = "5";
    /** 非法数据 **/
    public static final String TYPE_DATA = "2";
    /** MQ消息 **/
    public static final String TYPE_MQ = "9";
    /** 系统内部错误 **/
    public static final String TYPE_SERVER = "3";
    /** 其它错误 **/
    public static final String TYPE_OTHER = "4";
    /** 平台生命周期事件（启动/停止/重启/恢复） **/
    public static final String TYPE_PLATFORM_EVENT = "6";

    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 平台代码（关联 za_sys_platform.code） */
    @Excel(name = "平台代码")
    @ApiModelProperty("平台代码")
    private String pfCode;

    /** 类型 */
    @Excel(name = "类型")
    @ApiModelProperty("类型")
    private String type;

    /** 标题 */
    @Excel(name = "标题")
    @ApiModelProperty("标题")
    private String title;

    /** 处理内容 */
    @Excel(name = "处理内容")
    @ApiModelProperty("处理内容")
    private String content;

    /** 错误说明 */
    @Excel(name = "错误说明")
    @ApiModelProperty("错误说明")
    private String error;

    /** 记录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "记录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("记录时间")
    private Date logTime;



}
