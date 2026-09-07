package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 警情信息
 */
@Data
public class StatInfo implements Serializable {

    /**
     * 警情ID,本月内不会重复
     */
    private Long id;

    /**
     * 警情时间
     */
    private Long time;

    /**
     * 警情类型
     */
    private Integer type;

    /**
     * 警情类型
     */
    private String typeStr;

    /**
     * 警情事件
     *
     * 青鸟云平台 实际返回字段值 为var 而非 val
     * @since 2022-2-14
     */
    private Integer val;

    private Integer var;

    /**
     * 警情事件
     */
    private String valStr;

    public void setVar(Integer var){
        this.val = var;
    }
}
