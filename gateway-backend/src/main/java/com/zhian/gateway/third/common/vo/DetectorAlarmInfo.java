package com.zhian.gateway.third.common.vo;

import lombok.Data;

import java.util.Date;

/**
 * 青瞳主机信息
 */
@Data
public class DetectorAlarmInfo {
    private String deviceId;
    private String eventId;
    private String detectorSn;
    private Integer alarmType;
    private String alarmTypeName;
    private String alarmTypeDesc;
    private String picture1;
    private String picture2;
    private Integer zoneId;
    private Date time;
    private Boolean manualAlarm;
    private String resetWay;
}
