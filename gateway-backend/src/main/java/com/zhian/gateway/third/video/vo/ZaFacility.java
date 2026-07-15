package com.zhian.gateway.third.video.vo;

import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 设备信息对象 za_facility
 * 
 * @author yepanpan
 * @date 2024-04-10
 */
@ApiModel(value = "ZaFacility", description = "设备信息")
@Data
public class ZaFacility extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增长主键ID */
    @ApiModelProperty("${comment}")
    private Long id;

    /** 组织ID */
    @Excel(name = "组织区域")
    private String deptName;
    @ApiModelProperty("组织ID")
    @NotNull
    private Long deptId;

    /** 设备类型ID */
    @Excel(name = "设备类型")
    private String typeName;
    @ApiModelProperty("设备类型代码")
    private String typeCode;
    @ApiModelProperty("设备类型ID")
    @NotNull
    private Long typeId;
    private ZaFacilityType type;

    /** 系统类型ID */
    @Excel(name = "系统类型", type = Excel.Type.EXPORT)
    private String sysTypeName;
    private String sysTypeCode;
    @ApiModelProperty("系统类型ID")
    @NotNull
    private Long sysTypeId;

    /** 型号 */
    @Excel(name = "型号")
    private String modelName;
    private ZaFacilityModel model;
    @ApiModelProperty("型号")
    private Long modelId;

    /** 设备编码 */
    @Excel(name = "设备编码")
    @ApiModelProperty("设备编码")
    @NotNull
    private String facilityCode;

    /** 设备位置 */
    @Excel(name = "设备位置")
    @ApiModelProperty("设备位置")
    @NotNull
    private String position;

    /** 位置类型，1-室内，2-室外 */
    @Excel(name = "位置类型", dictType = "position_type")
    @ApiModelProperty("位置类型")
    @NotNull
    private String positionType;

    /** 经度 */
    @Excel(name = "经度")
    @ApiModelProperty("经度")
    private BigDecimal longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    @ApiModelProperty("纬度")
    private BigDecimal latitude;

    /** 建筑ID */
    @Excel(name = "建筑")
    private String buildingName;
    @ApiModelProperty("建筑ID")
    private Long buildingId;

    /** 楼层ID */
    @Excel(name = "楼层")
    private String floorName;
    private BigDecimal floorNum;
    @ApiModelProperty("楼层ID")
    private Long floorId;

    /** 房间ID */
    @Excel(name = "房间")
    private String roomName;
    @ApiModelProperty("房间ID")
    private Long roomId;

    /** X坐标 */
    @Excel(name = "X坐标")
    @ApiModelProperty("X坐标")
    private String pointX;

    /** Y坐标 */
    @Excel(name = "Y坐标")
    @ApiModelProperty("Y坐标")
    private String pointY;

    /** Y坐标 */
    @Excel(name = "Y坐标")
    @ApiModelProperty("Y坐标")
    private String pointZ;

    /** 回路号 */
    @Excel(name = "回路号")
    @ApiModelProperty("回路号")
    private Integer propLoop;

    /** 通道号 */
    @Excel(name = "通道号")
    @ApiModelProperty("通道号")
    private Integer propChannel;

    /** IP地址 */
    @Excel(name = "IP地址")
    @ApiModelProperty("IP地址")
    private String propIp;

    /** 端口 */
    @Excel(name = "端口")
    @ApiModelProperty("端口")
    private Long propPort;

    /** 设备登录用户名 */
    @Excel(name = "设备登录用户名")
    @ApiModelProperty("设备登录用户名")
    private String propUsername;

    /** 设备登录密码 */
    @Excel(name = "设备登录密码")
    @ApiModelProperty("设备登录密码")
    private String propPassword;

    /** RTSP流 */
    @Excel(name = "RTSP流")
    @ApiModelProperty("RTSP流")
    private String propRtsp;

    /** FLV流 */
    @Excel(name = "FLV流")
    @ApiModelProperty("FLV流")
    private String propFlv;

    /** 网关设备 */
    @Excel(name = "网关设备")
    private String propNetCode;
    @ApiModelProperty("网关设备")
    private Long propNetId;

    /** 上级设备 */
    @Excel(name = "上级设备")
    private String propParentCode;
    @ApiModelProperty("上级设备")
    private Long propParentId;

    /** 控制器 */
    @Excel(name = "控制器")
    private String propControllerCode;
    @ApiModelProperty("控制器")
    private Long propControllerId;

    /** 组网模式 */
    @Excel(name = "组网模式", dictType = "controller_net_mode")
    @ApiModelProperty("组网模式")
    private String propNetMode;

    /** 设备状态(0:在线，1:故障，2:离线) */
    @Excel(name = "设备状态", dictType = "run_status")
    @ApiModelProperty("设备状态(0:在线，1:故障，2:离线)")
    private String runStatus;

    /** 设备使用状态，0-正常，1-故障，2-维修中，3-停用 */
    @Excel(name = "设备使用状态", dictType = "use_status")
    @ApiModelProperty("设备使用状态，0-正常，1-故障，2-维修中，3-停用")
    private String useStatus;

    /** 设备报废状态，0-正常，1-报废 */
    @Excel(name = "设备报废状态", dictType = "scrap_status")
    @ApiModelProperty("设备报废状态，0-正常，1-报废")
    private String scrapStatus;

    /** 质保期限（月） */
    @Excel(name = "质保期限（月）", width = 30)
    @ApiModelProperty("质保期限（月）")
    private Integer scrapDate;

    /** 业务编码 */
    @Excel(name = "业务编码")
    @ApiModelProperty("业务编码")
    private String extCode;

    /** 归属平台 */
    @ApiModelProperty("归属平台")
    private String pfCode;

    /** 信号量 */
    @Excel(name = "信号量")
    @ApiModelProperty("信号量")
    private String rssi;

    /** 电量 */
    @Excel(name = "电量")
    @ApiModelProperty("电量")
    private String voltage;
}
