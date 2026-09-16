package com.zhian.gateway.third.others.mklora.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 铭控 LoRa 单路传感器读数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MkLoraSensorValue {

    /** 字段名，如 pressure / temperature */
    private String name;

    /** 工程单位 */
    private String unit;

    /** 原始整型/位模式值；浮点类型可为空 */
    private Long raw;

    /** 换算后的工程值 */
    private BigDecimal value;

    /** 无法换算时保留原始十六进制 */
    private String hex;
}
