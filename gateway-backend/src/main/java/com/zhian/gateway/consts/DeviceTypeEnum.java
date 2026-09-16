package com.zhian.gateway.consts;

/**
 * 常见设备类型
 */
public enum DeviceTypeEnum {
    WGP("WGP", "青瞳主机"),
    ICFD("ICFD", "图像探测设备"),
    WCB("WCB", "智慧云盒"),
    CAMERA("camera", "摄像机"),
    BRACELET("BRACELET", "智能手环"),
    ;

    private String code;
    private String name;

    private DeviceTypeEnum(String code, String name){
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
