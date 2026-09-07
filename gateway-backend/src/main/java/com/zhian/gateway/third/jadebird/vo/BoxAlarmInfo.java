package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

@Data
public class BoxAlarmInfo {
    private String deviceId;
    private Integer channel;
    private Integer eventType;
    private Integer eventState;
    private String picUrl;
    private String[] states = {"1","2"};


    public String getState(){
        return this.states[eventState];
    }

}
