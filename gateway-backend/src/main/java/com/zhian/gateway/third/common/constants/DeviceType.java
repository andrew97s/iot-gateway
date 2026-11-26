package com.zhian.gateway.third.common.constants;

/** 设备类型 */
public class DeviceType {
    /** 用传 */
    public static final String UITD = "UITD";

    /** HRP网关 */
    public static final String HRPWLG = "HRPWLG";

    /** 视频网关 */
    public static final String VAG = "VAG";

    /** 摄像机 */
    public static final String CAMERA = "camera";

    /**
     * 判断是否用传代码
     * @param code
     * @return
     */
    public static boolean isUITD(String code){
        return code.length() == 32 && code.startsWith("000000");
    }
}
