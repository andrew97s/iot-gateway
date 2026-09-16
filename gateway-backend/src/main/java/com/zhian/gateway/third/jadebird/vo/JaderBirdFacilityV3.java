package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author luping
 * Description: 青鸟云设备V3
 * @version 1.0
 * @date 2024/11/28 8:57
 */
@Data
public class JaderBirdFacilityV3 implements Serializable {
    //数据项类型2:设备；102：防火单位
    private Long objectClassCode;
    //设备ID(主键)，为空代表设备不存在
    private Long id;

    //设备编码地址/PSN/IMEI
    private String addr;

    //父级设备id
    private Long pid;


    //通讯方式，参考数据字典telecom为空代表子设备需依赖通讯设备/网关上云，不为空代表直连设备能独立上云
    private Long telecomCode;

    //通讯设备/网关ID
    private Long telecomId;

    //安装位置
    private String descr;

    //设备类型，参考数据字典facility_type
    private Long typeCode;

    //在线状态，0：离线；1：在线；2：未知
    private Long online;

    private Long sysCode; // 系统类型，可选

    //型号
    private Long modelCode;

    //实时值（模拟量）
    private List<MeterV3> meter;

    //状态
    private List<StateV3> state;

}
