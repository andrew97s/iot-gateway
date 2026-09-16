package com.zhian.gateway.third.dahua.common;

import com.zhian.gateway.third.common.constants.DeviceType;

/**
 * 大华ICC平台接入的设备类型
 */
public class IccDeviceType {

    /** 摄像机 **/
    public static final String CAMERA = DeviceType.CAMERA;

    /** 卡口 **/
    public static final String ANPR = "ANPR";

    /** 可视对讲 **/
    public static final String VI = "HTIntercom";

    /** 道闸 **/
    public static final String TSSG = "TSSG";

    /** 门禁一体机 **/
    public static final String ACSM = "ACSM";

    /** 门禁控制器 **/
    public static final String ACSC = "ACSC";

    /** 普通门禁 **/
    public static final String ACS = "ACS";
}
