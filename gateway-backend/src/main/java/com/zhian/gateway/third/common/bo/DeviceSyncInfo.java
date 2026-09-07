package com.zhian.gateway.third.common.bo;

import lombok.Data;

/**
 * 设备同步结果
 *
 * @author tongwenjin
 * @since 2026/5/18
 */

@Data
public class DeviceSyncInfo {

    private long syncCount;

    private boolean success;

    private String msg;

    public static DeviceSyncInfo fail(String msg) {
        DeviceSyncInfo info = new DeviceSyncInfo();
        info.setSuccess(false);
        info.setMsg(msg);
        return info;
    }

    public static DeviceSyncInfo success(long syncCount) {
        DeviceSyncInfo info = new DeviceSyncInfo();
        info.setSyncCount(syncCount);
        info.setSuccess(true);
        info.setMsg("OK");
        return info;
    }
}
