package com.zhian.gateway.third.others.mk.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 铭控 V3.3 上行报文
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Data
public class MkV3Msg {
    // IP
    private String sourceIp;
    // 指令 默认上传数据命令类型为 0x00
    private int command;
    // 12位设备编码
    private String deviceCode;
    // 设备类型code
    private int deviceType;
    // 设备类型名称
    private String deviceTypeName;
    // 数据类型
    private String kind;
    // 是否外接供电
    private boolean externalPower;
    // 电量百分比
    private int batteryPercent;
    // 信号强度
    private int rssi;
    // 采集间隔
    private int intervalSec;
    // 记录数量
    private int recordCount;
    // 告警状态
    private int alarmStatus;
    // 阈值
    private List<String> thresholdLows = new ArrayList<>();
    private List<String> thresholdHighs = new ArrayList<>();
    // 当前监测值
    private List<MkV3Value> currentValues = new ArrayList<>();
    // 记录时间
    private Date time;
}
