package com.zhian.gateway.consts;

/**
 * 默认值
 */
public class DefaultValue {
    /** 默认组织单位 **/
    public static final Long TOP_ORG =100L;

    /** 默认设备分类 **/
    public static final String FACILITY_TYPE ="currencyComponent";


    /** 未注册的事件归属告警代码 **/
    public static final String ALARM_TYPE ="450401";


    /** 默认数据添加人 **/
    public static final String CREATE_BY ="auto";

    public static void main(String[] args){
        String fsn = "1机 6-159";
        System.out.println(fsn.substring(0, fsn.indexOf("机")+1));
    }
}
