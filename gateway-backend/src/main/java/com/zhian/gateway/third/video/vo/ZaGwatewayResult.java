package com.zhian.gateway.third.video.vo;

import com.zhian.gateway.common.constant.HttpStatus;
import lombok.Data;

/**
 * 从消安平台查询视频网关的返回结果
 */
@Data
public class ZaGwatewayResult {
    private Integer code;
    private String msg;
    private ZaFacility[] data;

    public boolean isSuccess(){
        return code == HttpStatus.SUCCESS;
    }
}
