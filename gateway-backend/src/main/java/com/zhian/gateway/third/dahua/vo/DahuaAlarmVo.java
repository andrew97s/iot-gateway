package com.zhian.gateway.third.dahua.vo;

import lombok.Data;

/**
 * 大华摄像机告警信息
 */
@Data
public class DahuaAlarmVo {

    /** 设备代码  */
    private String code;

    /** 名称  */
    private String name;

    /** IP地址  */
    private String ip;

    /** 名称    */
    private String comment;

    /** 告警时间 **/
    private Long alarmTime;

    /** 告警类型 */
    private int alarmType;

    /** 告警图片 **/
    private String alarmImage;

    /** 1：报警产生，2：报警消失 **/
    private Integer alarmStat;
}
