package com.zhian.gateway.consts;

/**
 * 消息类型
 *
 * @author tongwenjin
 * @since 2022 /7/21
 */
public enum MessageType {

    /**
     * 告警消息.
     */
    WARNING_MESSAGE("WARNING_MESSAGE"),

    /**
     * 业务消息.
     */
    BUSINESS_MESSAGE("BUSINESS_MESSAGE"),


    /**
     * 原始消息.
     */
    SOURCE_MESSAGE("SOURCE_MESSAGE");

    final String code;

    MessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
