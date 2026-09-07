package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询设备直播流请求
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchDeviceStreamReq extends EzvizReq {

    private String deviceSerial;

    /**
     * 1-ezopen、2-hls、3-rtmp、4-flv
     */
    private Integer protocol = 4;

}
