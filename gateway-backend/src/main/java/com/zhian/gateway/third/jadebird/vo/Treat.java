package com.zhian.gateway.third.jadebird.vo;

/**
 * 青鸟云 处警 信息VO
 *
 * @author tongwenjin
 * @since 2022/5/6
 */
public class Treat {
    // 警情ID
    private long id;

    // 警情时间戳（秒）
    private long time;

    // 处情时间戳（秒）
    private String treatTime;

    // 处警人
    private String treator;

    // 处警类型编码
    private int treatType;

    // 处警类型
    private String treatTypeStr;
}
