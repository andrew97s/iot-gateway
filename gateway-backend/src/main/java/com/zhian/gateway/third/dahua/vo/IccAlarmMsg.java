package com.zhian.gateway.third.dahua.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * ICC平台告警消息
 */
@Data
public class IccAlarmMsg {
    private String category;
    private String method;
    private String subsystem;
    private String uuid;
    private String info;
    private String id;

    @Data
    public static class Info {
        //--------------------告警消息------------------------
        private String chnId;
        private String channelName;
        private String orgCode;
        private String orgName;
        private String deviceCode;
        private String deviceName;
        private String alarmCode;
        private String alarmPicture;
        private Integer nodeType;
        private Integer alarmGrade;
        private Long alarmDate;
        private Integer unitType;
        private Integer alarmType;
        private Integer channelSeq;
        private Integer alarmStat;
        private Boolean event;
        private Boolean isEvent;
        private String extend;

        //--------------------设备状态------------------------
        private String syncChannelStatus;
        private Integer offlineReason;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
        private Date updateTime;
        /** 1在线 0离线 **/
        private Integer status;
    }
}
