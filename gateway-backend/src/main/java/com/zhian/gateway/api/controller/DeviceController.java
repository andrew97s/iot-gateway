package com.zhian.gateway.api.controller;

import com.zhian.gateway.api.domain.VideoRequest;
import com.zhian.gateway.api.service.IDeviceService;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.vo.ControlVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备相关接口
 */
@RestController
@RequestMapping("/api/device/")
public class DeviceController extends BaseController {
    @Autowired
    private IZaSysDeviceService zaSysDeviceService;
    @Autowired
    private IDeviceService deviceService;

    @PostMapping("playUrl")
    public AjaxResult playUrl(@RequestBody VideoRequest video) {
        return deviceService.playUrl(video);
    }


    @PostMapping("control")
    public AjaxResult control(@RequestBody ControlVo controlVo) {
        return deviceService.control(controlVo);
    }

    /**
     * 分页查询接入设备列表
     */
    @ApiOperation("分页查询接入设备列表")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysDevice zaSysDevice)
    {
        startPage();
        List<ZaSysDevice> list = zaSysDeviceService.selectZaSysDeviceList(zaSysDevice);
        if(StringUtils.isNotEmpty(list)){
            list.forEach(e->e.setRemark(null));
        }
        return getDataTable(list);
    }

    @ApiOperation("强制网关向上推送设备列表")
    @GetMapping("push")
    public AjaxResult push(ZaSysDevice zaSysDevice) {
        return deviceService.push(zaSysDevice);
    }

}
