package com.zhian.gateway.third.video.roc_v2;

/**
 * 状态枚举
 *
 * @author tongwenjin
 * @since 2025-3-31
 */
public enum StatusEnum {

    SUCCESS(0 , "成功"),
    REJECTED (1 , "模组拒绝该命令"),
    MR_ABORTED(2 , "录入/验证算法已终止"),
    MR_FAILED4_CAMERA(4 , "相机打开失败"),
    MR_FAILED4_UNKNOWNREASON(5 , "未知错误"),
    MR_FAILED4_INVALIDPARAM(6 , "无效的参数"),
    MR_FAILED4_NOMEMORY(7 , "内存不足"),
    MR_FAILED4_UNKNOWNUSER(8 , "没有已录入的用户"),
    MR_FAILED4_MAXUSER(9 , "录入超过最大用户数量"),
    MR_FAILED4_FACEENROLLED(10 , "人脸已录入"),
    MR_FAILED4_LIVENESSCHECK(12 , "活体检测失败"),
    MR_FAILED4_TIMEOUT(13 , "录入或解锁超时"),
    MR_FAILED4_AUTHORIZATION(14 , "加密芯片授权失败"),
    MR_FAILED4_READ_FILE (19 , "读文件失败"),
    MR_FAILED4_WRITE_FILE(20 , "写文件失败"),
    MR_FAILED4_NO_ENCRYPT(21 , "通信协议未加密"),
    MR_FAILED4_NO_RGBIMAGE(23 , "RGB 图像没有 ready"),
    MR_FAILED4_JPGPHOTO_LARGE(24 , "JPG照片过大（照片注册）"),
    MR_FAILED4_JPGPHOTO_SMALL(25 , "JPG照片过小（照片注册）"),
    ;

    final int status;
    final String msg;

    StatusEnum(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public static StatusEnum getByCode(int code) {
        for (StatusEnum value : values()) {
            if (value.status == code) {
                return value;
            }
        }

        return MR_FAILED4_UNKNOWNREASON;
    }
}
