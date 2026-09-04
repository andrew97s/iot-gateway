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
    private String sourceIp;
    private int command;
    private String deviceCode;
    private int deviceType;
    private String deviceTypeName;
    private String kind;
    private String unit;
    private int batteryRaw;
    private boolean externalPower;
    private int batteryPercent;
    private int rssi;
    private int intervalSec;
    private int recordCount;
    private int alarmStatus;
    private String thresholdLow;
    private String thresholdHigh;
    private String value;
    private List<String> currentValues = new ArrayList<>();
    private List<String> historyValues = new ArrayList<>();
    private long utc;
    private Date time;
}
