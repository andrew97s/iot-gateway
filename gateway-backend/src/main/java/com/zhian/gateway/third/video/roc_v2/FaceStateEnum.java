package com.zhian.gateway.third.video.roc_v2;

/**
 * 人脸识别状态枚举
 *
 * @author tongwenjin
 * @since 2025-4-1
 */
public enum FaceStateEnum {

    FACE_STATE_NORMAL("0" , "人脸正常"),
    FACE_STATE_NOFACE("1" , "未检测到人脸"),
    FACE_STATE_TOOUP("2" , "人脸太靠近图片上边沿，未能录入"),
    FACE_STATE_TOODOWN("3" , "人脸太靠近图片下边沿，未能录入"),
    FACE_STATE_TOOLEFT("4" , "人脸太靠近图片左边沿，未能录入"),
    FACE_STATE_TOORIGHT("5" , "人脸太靠近图片右边沿，未能录入"),
    FACE_STATE_FAR("6" , "人脸距离太远，未能录入"),
    FACE_STATE_CLOSE("7" , "人脸距离太近，未能录入"),
    FACE_STATE_EYEBROW_OCCLUSION("8" , "眉毛遮挡"),
    FACE_STATE_EYE_OCCLUSION("9" , "眼睛遮挡"),
    FACE_STATE_FACE_OCCLUSION("10" , "脸部遮挡"),
    FACE_STATE_DIRECTION_ERROR("11" , "录入人脸方向错误"),
    FACE_STATE_EYE_CLOSE_STATUS_OPEN_EYE("12" , "在闭眼模式检测到睁眼状态"),
    FACE_STATE_EYE_CLOSE_STATUS("13" , "闭眼状态"),
    FACE_STATE_EYE_CLOSE_UNKNOW_STATUS("14" , "在闭眼模式检测中无法判定睁闭眼状"),
    UNKNOW_STATUS("-1" , "未知状态"),
    ;

    String code ;
    String name;

    FaceStateEnum(String code ,String name) {
        this.code = code;
        this.name = name;
    }

    public static FaceStateEnum getByCode(String code) {
        for (FaceStateEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }

        return UNKNOW_STATUS;
    }
}
