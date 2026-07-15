package com.zhian.gateway.consts;

public interface WebSocketMsgType {

    /**
     * 告警信息
     */
    String WARNING_MESSAGE = "warningMessage";

    /**
     * 设备数量
     */
    String FACILITY_ACCOUNT = "facilityCount";

    /**
     * 防疫通行信息
     */
    String HEALTH_MESSAGE = "healthMessage";

    /**
     * 防疫通行数量
     */
    String HEALTH_ACCOUNT = "healthCount";

    /**
     * 设备状态发生改变事件
     */
    String FACILITY_STATE = "facilityState";

    /**
     * 强制刷新页面数据
     */
    String REFRESH_PAGE = "refreshPage";


    /**
     * 输入输出模块状态发生改变事件
     */
    String CHAIN_STATUS = "chainStatus";
}
