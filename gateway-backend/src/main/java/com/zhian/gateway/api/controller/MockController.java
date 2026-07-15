package com.zhian.gateway.api.controller;

import com.github.pagehelper.PageHelper;
import com.zhian.gateway.api.domain.MockData;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysEvent;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysEventService;
import com.zhian.gateway.third.ThirdHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * 设备相关接口
 */
@RestController
@RequestMapping("/api/mock/")
public class MockController extends BaseController {
    @Autowired
    private IZaSysDeviceService deviceService;
    @Autowired
    private IZaSysEventService zaSysEventService;
    @Resource(name = "jadebirdInnerHandler")
    private ThirdHandler jadebirdInnerHandler;

    @PostMapping("jb")
    public AjaxResult alarm(@RequestBody MockData data){
        if(!data.isAuth()){
            return AjaxResult.error("非法请求");
        }
        ZaSysDevice sysDevice = getDevice(data);
        if(sysDevice == null){
            return AjaxResult.error("没有找到设备");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"event\":\"");
        if(StringUtils.isNotEmpty(data.getAlarmEvent())) {
            //告警
            ZaSysEvent ec = new ZaSysEvent();
            ec.setVendorCode("jb");
            ec.setSourceCode(data.getAlarmEvent());
            List<ZaSysEvent> zaSysEvent = zaSysEventService.selectZaSysEventList(ec);
            if(StringUtils.isEmpty(zaSysEvent)){
                throw new ServiceException("不支持的事件类型: " + data.getAlarmEvent());
            }
            sb.append("alarm\",\"stat\":[{");
            sb.append("\"id\":4530063,\"time\":").append(System.currentTimeMillis()/1000)
                    .append(",\"type\":1,\"typeStr\":\"火警\",\"val\":")
                    .append(zaSysEvent.get(0).getSourceCode()).append(",\"valStr\":\"").append(zaSysEvent.get(0).getComment()).append("\"");
            sb.append("}],\"facility\":{");
        }else{
            //巡检数据
            sb.append("heartbeat\",\"stat\":[{").append("}],,\"facility\":{");
            sb.append("\"analogType\":\"").append(data.getAnalogType())
                    .append("\",\"analogValue\":\"").append(data.getAnalogValue()).append("\",");
        }
        sb.append("\"addrStr\":\"").append(sysDevice.getCode())
                .append("\",\"descr\":\"").append(sysDevice.getName())
                .append("\",\"facilitiesTypeCode\":\"").append(sysDevice.getType())
                .append("\",\"net\":\"").append(sysDevice.getNet()).append("\"}")
                .append("}");
        jadebirdInnerHandler.processMsg(sb.toString());
        return AjaxResult.success();
    }

    private ZaSysDevice getDevice(MockData data){
        if(data.getFacilityId() != null){
            return deviceService.selectZaSysDeviceById(data.getFacilityId());
        }

        PageHelper.startPage(1, 50, "t.update_time DESC");
        ZaSysDevice dc = new ZaSysDevice();
        dc.setType(data.getFacilityType());
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        if(list == null || list.size() == 0){
            return null;
        }

        if(list.size() == 1){
            return list.get(0);
        }
        Random rd = new Random();
        return list.get(rd.nextInt(list.size()));
    }
}
