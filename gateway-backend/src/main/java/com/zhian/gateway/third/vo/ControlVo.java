package com.zhian.gateway.third.vo;

import com.zhian.gateway.sys.domain.ZaSysDevice;
import lombok.Data;

/**
 * 反向控制参数
 */
@Data
public class ControlVo {
    /**  复位 */
    public static final String CMD_RESET = "reset";
    /**  消音 */
    public static final String CMD_MUTE = "mute";
    /**  启停 */
    public static final String CMD_POWER = "power";
    /**  屏蔽 */
    public static final String CMD_SHIELD = "shield";
    /**  手自动切换 */
    public static final String CMD_MANUAL = "manual";
    /** 拉流  */
    public static final String CMD_STREAM = "stream";
    /** 回放  */
    public static final String CMD_PLAY_BACK = "playback";
    /** 录像查询  */
    public static final String CMD_RECORDS = "records";
    /** 拍照  */
    public static final String CMD_SNAP = "snap";
    /** 云台控制  */
    public static final String CMD_PTZ = "ptz";
    /** 配置下发  */
    public static final String CMD_SET = "set";
    /** 查询  */
    public static final String CMD_QRY = "qry";

    /** 事件 */
    private String eventId;

    private Long deviceId;

    private String code;

    private String net;

    /** 指令 */
    private String command;

    /** 值 */
    private String value;

    private ZaSysDevice device;
}
