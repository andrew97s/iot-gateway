package com.zhian.gateway.third.common.constants;


/**
 * 自定义告警类别
 */
public enum AlarmType {
    ONLINE("902", "上线"),
    OFFLINE("901", "离线"),
    JB_ONLINE("54", "上线"),
    JB_OFFLINE("55", "离线"),
            ;

    private String code;
    private String name;

    private AlarmType(String code, String name){
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
