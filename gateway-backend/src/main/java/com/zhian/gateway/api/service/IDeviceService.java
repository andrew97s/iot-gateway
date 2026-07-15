package com.zhian.gateway.api.service;

import com.zhian.gateway.api.domain.VideoRequest;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.third.vo.ControlVo;

/**
 * 设备业务接口
 */
public interface IDeviceService {

    /**
     * 查询视频播放地址
     * @param videoRequest
     * @return
     */
    public AjaxResult playUrl(VideoRequest videoRequest);


    /**
     * 反向控制
     * @param controlVo
     * @return
     */
    public AjaxResult control(ControlVo controlVo);


    /**
     * 强制推送设备信息进行同步
     * @param zaSysDevice
     * @return
     */
    public AjaxResult push(ZaSysDevice zaSysDevice);
}
