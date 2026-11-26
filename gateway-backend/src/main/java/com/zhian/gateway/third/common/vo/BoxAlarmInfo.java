package com.zhian.gateway.third.common.vo;

import lombok.Data;

/**
 * 青鸟云盒告警消息
 */
@Data
public class BoxAlarmInfo {
    private String deviceId;
    private Integer channel;
    /** 告警类别：火焰检测 0，烟雾检测 1，安全帽检测 2，室内通道占用检测 3，人员离岗检测 4，危险区域入侵检测 5，室外消防通道占用检测 6
     人员聚集检测 7，电动自行车检测 8 ，跌倒检测 9，人员巡岗检测 10，抽烟检测 11，打电话检测 12，电动车电瓶监测 13 **/
    private Integer algorithmId;
    /**兼容旧的告警类型 **/
    private Integer eventType;
    /** 0产生 1解除 **/
    private Integer eventState;
    private String picUrl;
}
