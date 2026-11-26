package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class
MonitorMsg implements Serializable {

    private String event;

    private Facility facility;

    private FireUnit fireUnit;

    private List<StatInfo> stat;

    private Treat treat;

    public enum Event {
        /**
         * 报警
         */
        ALARM,

        /**
         * 处警
         */
        TREAT,

        /**
         * 无线设施心跳
         */
        HEARTBEAT,

        /**
         * 注册
         */
        INSERT,

        /**
         * 删除
         */
        DELETE,

        /**
         * 未知事件
         */
        UNDIFINED;
    }

    public Event getEventType() {
        switch (event) {
            case "alarm":
                return Event.ALARM;
            case "treat":
                return Event.TREAT;
            case "heartbeat":
                return Event.HEARTBEAT;
            case "insert":
                return Event.INSERT;
            case "delete":
                return Event.DELETE;
            default:
                return Event.UNDIFINED;
        }
    }
}
