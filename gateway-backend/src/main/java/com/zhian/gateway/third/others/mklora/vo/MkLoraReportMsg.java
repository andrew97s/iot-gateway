package com.zhian.gateway.third.others.mklora.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 铭控 LoRa 主动上报解析结果（命令 0x10）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MkLoraReportMsg {

    private String sourceIp;

    /** 4 字节设备编号原始值 */
    private long deviceId;

    /**
     * 铭牌后 8 位 ID（deviceId % 100000000），同时对应 LoRaWAN ADDR。
     */
    private String deviceCode;

    private int batteryRaw;
    /** BIT7：0 电池 / 1 外供电 */
    private boolean externalPower;
    /** BIT6-5：供电方式 0 双供电 / 1 单电池 / 2 单外供 / 3 保留 */
    private int supplyMode;
    /** 电池电量百分比，5% 步进 */
    private int batteryPercent;

    /** 信号强度，单位 dBm */
    private int signalDbm;

    private int sensorType;
    private String sensorTypeName;

    private int alarmRaw;
    private boolean lowAlarm;
    private boolean highAlarm;
    private boolean batteryAlarm;
    private boolean sensorFault;
    private boolean hardwareFault;
    private boolean sensor2LowAlarm;
    private boolean sensor2HighAlarm;
    private boolean sensor3Alarm;

    /** 是否存在任意报警位 */
    private boolean alarmed;

    @Builder.Default
    private List<MkLoraSensorValue> values = new ArrayList<>();

    /** 实时数据区原始十六进制 */
    private String dataHex;

    /** 完整帧十六进制 */
    private String rawHex;

    private Date time;
}
