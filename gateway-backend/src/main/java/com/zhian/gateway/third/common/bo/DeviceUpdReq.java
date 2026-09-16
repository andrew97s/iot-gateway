package com.zhian.gateway.third.common.bo;

import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.third.common.BasePlatformHandler;
import lombok.Data;

/**
 * 设备更新请求
 *
 * @author tongwenjin
 * @since 2026/5/18
 */
@Data
public class DeviceUpdReq {

    private ZaSysDevice device;

    private String action;

    private String remark;

    private BasePlatformHandler<?> handler;

    public static DeviceUpdReq newInstance(ZaSysDevice device, String action, String remark, BasePlatformHandler<?> handler) {
        DeviceUpdReq req = new DeviceUpdReq();
        req.device = device;
        req.action = action;
        req.remark = remark;
        req.handler = handler;

        return req;
    }

    public static DeviceUpdReq newAddReq(ZaSysDevice device, String remark) {
        return newInstance(device, "add", remark, null);
    }

    public static DeviceUpdReq newUpdReq(ZaSysDevice device, String remark) {
        return newInstance(device, "upd", remark, null);
    }

    public static DeviceUpdReq newDelReq(ZaSysDevice device, String remark) {
        return newInstance(device, "del", remark, null);
    }

    public static DeviceUpdReq newOnlineReq(ZaSysDevice device, String remark) {
        return newInstance(device, "online", remark, null);
    }

    public static DeviceUpdReq newOfflineReq(ZaSysDevice device, String remark) {
        return newInstance(device, "offline", remark, null);
    }
}
