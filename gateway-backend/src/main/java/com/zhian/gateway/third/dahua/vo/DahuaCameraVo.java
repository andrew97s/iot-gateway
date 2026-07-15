package com.zhian.gateway.third.dahua.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 大华摄像机信息
 */
@Data
public class DahuaCameraVo {

    /** 入库的设备ID    */
    @JsonIgnore
    private Long deviceId;

    /** 序列号  */
    private String sn;

    /** 自动注册的ID    */
    private String regId;

    /** 名称    */
    private String name;

    /** 型号  */
    private String model;

    /** IP地址   */
    private String ip;

    /** 端口  */
    private int port;

    /** 搜索到的设备 **/
    @JsonIgnore
    private Boolean search = false;
    /** 主动注册的设备 **/
    @JsonIgnore
    private Boolean register = false;
    private Long regTime;



    /** 最后告警类型 */
    @JsonIgnore
    private int alarmType;

    /** 设备代码:主动注册的是regId,搜索到的是sn **/
    @JsonIgnore
    public String getCode(){
        return register ? regId : sn;
    }
}
