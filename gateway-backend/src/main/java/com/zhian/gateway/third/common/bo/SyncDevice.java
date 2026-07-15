package com.zhian.gateway.third.common.bo;

import lombok.Builder;
import lombok.Data;

/**
 * 设备同步B0
 *
 * @author tongwenjin
 * @since 2024 /8/1
 */
@Data
@Builder
public class SyncDevice {

    Long id;

    /**
     * The Code.
     */
    String code;

    /**
     * The Name.
     */
    String name;

    /**
     * The Net.
     */
    String net;

    /**
     * The Model.
     */
    String model;

    /**
     * The Type code.
     */
    String typeCode;

    /**
     * The Pf code.
     */
    String pfCode;

    /**
     * The Wireless.
     */
    String wireless;

    /**
     * The Ip.
     */
    String ip;

    /**
     * 是否在线（1在线，0离线）
     */
    String online;

    /**
     * The Remark.
     */
    String remark;
}
