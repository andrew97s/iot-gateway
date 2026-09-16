package com.zhian.gateway.third.others.cr.vo;

import lombok.Data;

/**
 * @author tongwenjin
 * @since 2025-3-17
 */

@Data
public class CrResp {

    private int code;

    private String msg;

    private Data data;

    @lombok.Data
    public static class Data {
        private String carId;

        private String carType;
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
