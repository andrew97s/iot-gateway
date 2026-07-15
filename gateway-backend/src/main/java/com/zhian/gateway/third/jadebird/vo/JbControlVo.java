package com.zhian.gateway.third.jadebird.vo;

import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import lombok.Data;

/**
 * 设备控制对象
 * mute|reset value0
 * power 1开 0关
 */
@Data
public class JbControlVo {
    public static String OPTION_MUTE = "mute";
    public static String OPTION_RESET = "reset";
    public static String OPTION_POWER = "power";

    public static String VALUE_OPEN = "1";
    public static String VALUE_CLOSE = "0";

    private String ak; // 是 String 注册用户返回值 any:绕过校验
    private String  uuid; // 是 String 操作唯一编码 记录操作唯一编码
    private String  option; // 是 String 详见操作类型枚举
    private String  value; // 是 String 详见操作类型枚举
    private String  dzCode; // 否 String 系统唯一标识

    // private String deviceId; // 否 String 设备编码
    // private String  keep; // 否 String 设备源 dzCode或者(keep和deviceId)二选一

    /**
     * 创建简单的请求对象
     * @param option
     * @param value
     * @param dzCode
     * @return
     */
    public static JbControlVo create(String option, String value, String dzCode){
        JbControlVo request = new JbControlVo();
        request.setAk("any");
        request.setDzCode(dzCode);
        // 控制类型
        request.setOption(option);
        // 雪花ID
        request.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        // 操作 value
        request.setValue(value);
        return request;
    }
    /**
     * 创建简单的请求对象
     * @param option
     * @param dzCode
     * @return
     */
    public static JbControlVo create(String option,String dzCode){
        JbControlVo request = new JbControlVo();
        request.setAk("any");
        request.setDzCode(dzCode);
        // 控制类型
        request.setOption(option);
        // 雪花ID
        request.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        // 操作 value
        request.setValue("1");
        return request;
    }
}
