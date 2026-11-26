package com.zhian.gateway.third.others.mk.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 铭控 V3.3 单路监测值。
 *
 * @author tongwenjin
 * @since 2026/9/4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MkV3Value {

    /** 通道号，从 1 开始 */
    private int channel;

    /** 字段名，如 pressure / temperature */
    private String name;

    /** 工程单位 */
    private String unit;

    /** za_monitor_type 映射别名 */
    private String monitorAlias;

    /** 换算后的工程值 */
    private String value;

    private String thresholdLow;

    private String thresholdHigh;
}
