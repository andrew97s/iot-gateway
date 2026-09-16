package com.zhian.gateway.third.video.roc_v2;

/**
 * 控制命令枚举
 *
 * @author tongwenjin
 * @since 2025-3-31
 */
public enum CmdEnum {

    RESET("0x10", "重置模组"),
    MID_GETSTATUS("0x11", "获取墨子状态"),
    MID_VERIFY("0x12", "验证人脸"),
    MID_ENROLL("0x13", "录入人脸(多角度)"),
    MID_ENROLL_SINGLE("0x1D", "录入人脸(单角度)"),
    MID_DELUSER("0x20", "删除指定用户"),
    MID_DELALL("0x21", "删除所有用户"),
    MID_GETUSERINFO("0x22", "获取用户详情"),
    MID_FACERESET("0x23", "人脸重置"),
    MID_GET_ALL_USERID("0x24", "获取所用用户"),
    MID_ENROLL_ITG("0x26", ""),
    MID_GET_VERSION("0x30", "获取软件版本"),
    MID_GET_SN("0x93", "获取设备序列号"),
    MID_ENROLL_WITH_PHOTO("0xF7", "照片录入"),
    UNKNOWN("0xFF", "未知操作");

    final String code;

    final String name;

    CmdEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static CmdEnum getByCode(String code) {
        code = "0x" +  code.toUpperCase();
        for (CmdEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }

        return UNKNOWN;
    }
}
