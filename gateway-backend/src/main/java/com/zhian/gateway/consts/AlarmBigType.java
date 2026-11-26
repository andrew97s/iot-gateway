package com.zhian.gateway.consts;

public enum AlarmBigType {
    FIRE("4501", "火警"),
    WARNING("4502", "预警"),
    FAULT("4503", "故障"),
    EVENT("4504", "事件"),;
    private String code;
    private String name;

    AlarmBigType(String code, String name){
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
