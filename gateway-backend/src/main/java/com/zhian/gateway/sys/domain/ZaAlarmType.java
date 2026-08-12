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
 * 标准告警类型 za_alarm_type
 *
 * 网关的标准告警类型字典。接入插件收到的厂商告警码（别名）
 * 通过 aliases 映射到标准告警类型后，再组装统一告警消息同步上级平台。
 */
@ApiModel(value = "ZaAlarmType", description = "标准告警类型")
@Data
@TableName("za_alarm_type")
public class ZaAlarmType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键")
    private Long id;

    /** 类型编码（网关内唯一，同步上级平台使用） */
    @ApiModelProperty("类型编码")
    private String code;

    /** 类型名称 */
    @ApiModelProperty("类型名称")
    private String name;

    /** 告警级别：1 火警 , 2 预警 ,  3 故障 , 4 事件 */
    @ApiModelProperty("告警级别")
    private Integer type;

    /**
     * 插件别名映射 JSON 数组：[{"pfCode":"jb","alias":"4"},{"pfCode":"hikvision","alias":"fireAlarm"}]
     * 多个插件别名可映射到同一个标准类型
     */
    @ApiModelProperty("插件别名映射(JSON)")
    private String aliases;

    @ApiModelProperty("代表可以恢复当前告警的事件编码(JSON)")
    private String recoveryCode;

    @ApiModelProperty("代表当前告警可以恢复的事件编码(JSON)")
    private String cancelCode;

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
