package com.zhian.gateway.third.video.roc.common;

/**
 * ROC相关常量
 *
 * @author tongwenjin
 * @since 2025-2-18
 */
public interface RocConstants {

    String MSG_GET_DEVICE_INFO = "{\"method\": \"get\",\"uri\": \"/ROC/System/deviceInfo\"}";
    String GROUP_NAME = "打卡登录";
    String METHOD_GET = "GET";
    String METHOD_PUT = "PUT";
    String METHOD_POST = "POST";
    String URI_DEVICE_INFO = "/ROC/System/deviceInfo";
    String URI_SESSION = "/ROC/Security/Session";
    String URI_FACE_RULE = "/ROC/Intelligent/Channels/1/faceRule";
    String URI_SET_PUSH = "/ROC/System/Network/httpPushCfg";
    String URI_FACE_GROUP_ADD = "/ROC/Intelligent/Channels/1/FaceRecMgrDatabase/add";
    String URI_FACE_ADD = "/ROC/Intelligent/Channels/1/FaceRecTemDatabase/add";
    String URI_FACE_MODIFY = "/ROC/Intelligent/Channels/1/FaceRecTemDatabase/modify";
    String URI_FACE_DELETE = "/ROC/Intelligent/Channels/1/FaceRecTemDatabase/delete";
    String URI_FACE_SEARCH = "/ROC/Intelligent/Channels/1/FaceRecTemDatabase/search";

    //事件类型 AlarmIn”,"Motion","Human","Absent","AreaIn","AreaOut","AreaLinger","VirtualLine","Passenger","FaceCapture","FaceCompare","Fire","E
    //Bike"等
    String EVENT_FACE_COMPARE = "FaceCompare";
    String EVENT_FACE_ENROLL = "FaceEnroll";
    String EVENT_FACE_CAPTURE = "FaceCapture";
}
