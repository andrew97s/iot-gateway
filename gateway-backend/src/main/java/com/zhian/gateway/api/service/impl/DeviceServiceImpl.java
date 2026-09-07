package com.zhian.gateway.api.service.impl;

import com.github.pagehelper.PageHelper;
import com.zhian.gateway.api.domain.VideoRequest;
import com.zhian.gateway.api.service.IDeviceService;
import com.zhian.gateway.common.core.cache.Cache;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.consts.CascadeConst;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备接口
 */
@Service
@Slf4j
public class DeviceServiceImpl implements IDeviceService {
    public static final String CACHE_KEY = "device_push";
    public static final Long PUSH_PERIOD = 60 * 60 * 1000L;
    @Autowired
    private IZaSysDeviceService deviceService;
    @Autowired
    private IZaSysPlatformService zaSysPlatformService;
    @Autowired
    private Cache cache;

    @Override
    public R playUrl(VideoRequest videoRequest) {
        ControlVo controlVo = new ControlVo();
        controlVo.setEventId(SnowflakeIdWorker.getInstance().nextStringId());
        controlVo.setDeviceId(videoRequest.getDeviceId());
        controlVo.setCommand(ControlVo.CMD_STREAM);
        controlVo.setValue(videoRequest.getProtocol()+"_"+videoRequest.getVideoType());

        return control(controlVo);
    }


    /**
     * 反向控制
     * @param controlVo
     * @return
     */
    public R control(ControlVo controlVo){
        ZaSysDevice zaSysDevice = null;
        if(controlVo.getDeviceId() != null){
            zaSysDevice = deviceService.selectZaSysDeviceById(controlVo.getDeviceId());
        }
        if(zaSysDevice == null && controlVo.getCode() != null){
            zaSysDevice = deviceService.selectZaSysDeviceByCode(controlVo.getCode(), controlVo.getNet());
        }
        if(zaSysDevice == null){
            throw new ServiceException("操作失败,反控设备("+controlVo.getCode()+")不存在!");
        }
        controlVo.setDevice(zaSysDevice);

        log.info("device control: {}", controlVo);
        //先判断是否是级联设备
        if(zaSysDevice.getCascadeId() == null || zaSysDevice.getCascadeId().intValue() == 0){
            //非级联设备
            ZaSysPlatform zaSysPlatform = zaSysPlatformService.selectZaSysPlatformByCode(zaSysDevice.getPfCode());
            if(zaSysPlatform == null){
                return R.error("未找到网关信息");
            }
            return ThirdApplicationRunner.getHandler(zaSysPlatform.getCode()).control(controlVo);
        }else{
            if(StringUtils.isEmpty(controlVo.getEventId())){
                controlVo.setEventId(SnowflakeIdWorker.getInstance().nextStringId());
            }
            //级联设备
            ThirdApplicationRunner.getHandler(CascadeConst.PLATFORM_CODE).control(controlVo);
            try {
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(300);
                    R result = cache.getCacheMapValue(CascadeConst.CACHE_MAP_KEY, controlVo.getEventId());
                    if(result != null){
                        return result;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                return R.error("接口无响应");
            }
            return R.error("接口超时");
        }
    }


    /**
     * 强制推送设备信息进行同步
     * @param zaSysDevice
     * @return
     */
    @Override
    public R push(ZaSysDevice zaSysDevice){
        new Thread(()->{
            int page = 1;
            int limit = 3;
            ThirdHandler gatewayHandler = SpringUtils.getBean("messageSyncHandler");
            Map<String, ZaSysPlatform> platformMap = zaSysPlatformService.selectZaSysPlatformList(new ZaSysPlatform())
                    .stream().collect(Collectors.toMap(ZaSysPlatform::getCode, ZaSysPlatform->ZaSysPlatform));
            try{
                while(true){
                    PageHelper.startPage(page, limit);
                    List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(zaSysDevice);
                    if(StringUtils.isEmpty(list)){
                        break;
                    }
                    for(ZaSysDevice sysDevice:list){
                        ZaSysPlatform zaSysPlatform = platformMap.get(sysDevice.getPfCode());
                        if(zaSysPlatform == null){
                            log.error("未找到平台 {} 信息", sysDevice.getPfCode());
                        }else {

                            ThirdHandler thirdHandler = ThirdApplicationRunner.getHandler(zaSysPlatform.getCode());
                            MqMessage.Facility facility = new MqMessage.Facility();
                            facility.setType(sysDevice.getType());
                            facility.setCode(sysDevice.getCode());
                            facility.setName(sysDevice.getName());
                            facility.setModel(sysDevice.getModel());
                            facility.setNet(sysDevice.getNet());
                            facility.setWireless(sysDevice.getWireless() != null && sysDevice.getWireless().equalsIgnoreCase("1"));
                            facility.setOnLine(sysDevice.getOnline() != null && sysDevice.getOnline().equalsIgnoreCase("1"));

                            MqMessage message = MqMessage.createDevice(sysDevice.getId(), thirdHandler.getProtocol(), facility, sysDevice.getRemark());
                            message.setEventType(MqMessage.DEVICE_ADD);
                            gatewayHandler.processMsg(message);
                        }
                    }
                    if(list.size() < limit){
                        break;
                    }
                    page++;
                    Thread.sleep(1000);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
        return R.success("设备信息开始推送中");
    }

}
