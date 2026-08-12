package com.zhian.gateway.sys.domain;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 接入消息对象 za_sys_message
 * 
 * @author yepanpan
 * @date 2024-07-10
 */
@ApiModel(value = "ZaSysMessage", description = "接入消息")
@Data
public class ZaSysMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 平台 */
    @Excel(name = "平台")
    @ApiModelProperty("平台")
    private String pfCode;

    /** 设备 */
    @Excel(name = "设备")
    @ApiModelProperty("设备")
    private Long deviceId;

    /** 编号 */
    @Excel(name = "编号")
    @ApiModelProperty("编号")
    private String deviceCode;

    /** 类别 */
    @Excel(name = "类别")
    @ApiModelProperty("类别")
    private String type;

    /** 消息内容（接入插件的原始报文） */
    @Excel(name = "消息内容")
    @ApiModelProperty("消息内容(原始报文)")
    private String content;

    /** 转换后的统一消息（JSON，多条时为JSON数组） */
    @ApiModelProperty("统一消息内容")
    private String unifiedContent;

    /** 处置完成 */
    @Excel(name = "处置状态")
    @ApiModelProperty("处置状态")
    private String handleStatus;

    @ApiModelProperty("处置结果")
    private String handleResult;

    /** 推送状态：sent=成功, failed=失败, null=未知 */
    @Excel(name = "推送状态")
    @ApiModelProperty("推送状态：sent/failed")
    private String sendStatus;

    @ApiModelProperty("推送结果")
    private String sendResult;

    /** 重试次数 */
    @Excel(name = "重试次数")
    @ApiModelProperty("重试次数")
    private Integer retryCount;

    /** 最后重试时间 */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("最后重试时间")
    private java.util.Date retryTime;

    /** 推送状态常量 */
    public static final String SEND_STATUS_SENT    = "sent";
    public static final String SEND_STATUS_FAILED  = "failed";

    /** 统一消息 messageId（非表字段，列表/详情展示用） */
    @ApiModelProperty(hidden = true)
    private String messageId;

    /** 消息摘要（非表字段） */
    @ApiModelProperty(hidden = true)
    private String summary;

    /** 上级同步成功数 / 总数（非表字段） */
    @ApiModelProperty(hidden = true)
    private Integer syncSuccess;
    @ApiModelProperty(hidden = true)
    private Integer syncTotal;
    @ApiModelProperty(hidden = true)
    private String syncLabel;

    private Long costTime;
}
