package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询设备列表请求
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchDeviceReq extends EzvizReq {

    private Integer pageStart = 0;

    private Integer pageSize = 100;
}
