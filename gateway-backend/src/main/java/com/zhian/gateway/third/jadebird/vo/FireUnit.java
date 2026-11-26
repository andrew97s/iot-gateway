package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 社会单位
 */
@Data
public class FireUnit implements Serializable {

    /**
     * 社会单位ID
     */
    private Integer id;

    /**
     * 社会单位名称
     */
    private String name;

    /**
     * 社会单位地址
     */
    private String address;

    private Float latitude;

    /**
     * 青鸟云平台 实际返回字段值 为longtitude 而非 longitude
     *
     * @since 2022-2-14
     */
    private Float longtitude;

}
