package com.zhian.gateway.third.dahua;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.framework.disruptor.DisruptorUtil;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.third.dahua.vo.DahuaAlarmVo;
import com.zhian.gateway.third.dahua.vo.IccAlarmMsg;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 拉从视频网关对接的大华告警
 *
 * @author yepanpan
 */
@RestController
@RequestMapping("/dahua")
@Slf4j
public class DahuaInnerApi {

    @Autowired
    private DhSdkHandler dhSdkHandler;
    @Autowired
    private DhIccHandler dhIccHandler;
    @Autowired
    private IZaSysDeviceService deviceService;

    /** 服务端口 **/
    public static Integer wsPort;

    @Value("${zhian.server.port}")
    public void setWsPort(Integer wsPort) {
        this.wsPort = wsPort;
    }

    /**
     * 大华告警
     * @param deviceID
     * @param dhClientID
     * @param alarmType
     * @param multipartFile
     */
    @PostMapping("/jz")
    public R alarmInfo(@RequestParam("deviceID") String deviceID,
                                @RequestParam("dhClientID") String dhClientID,
                                @RequestParam("alarmType") Integer alarmType,
                                @RequestPart("eventImage") MultipartFile multipartFile) {
        log.info("----接收到[大华告警]告警消息 : deviceID[{}],alarmType[{}]",  deviceID, alarmType);

        if(alarmType == null || alarmType == 0){
            return R.error("信息有误");
        }

        /*
        EmAlarmType alarmTypeEm = EmAlarmType.getAlarm(alarmType);
        if(alarmTypeEm == null){
            log.error("摄像机{}未关注的事件[{}]-[{}]将忽略", dhClientID, alarmType, String.format("0x%x",alarmType));
            return R.success( "告警成功");
        }
         */

        DisruptorUtil.push(()->{
            DahuaAlarmVo dahuaAlarmVo = new DahuaAlarmVo();
            dahuaAlarmVo.setAlarmType(alarmType);
            //dahuaAlarmVo.setComment(alarmTypeEm.getName());
            dahuaAlarmVo.setAlarmTime(System.currentTimeMillis());
            dahuaAlarmVo.setAlarmImage(saveImage(multipartFile, dhClientID, alarmType));

            log.info("图片{}", dahuaAlarmVo.getAlarmImage());
            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(deviceID, null);
            if(zaSysDevice == null){
                log.error("大华摄像机{}-{}未注册", deviceID, dhClientID);
                return;
            }
            dahuaAlarmVo.setName(zaSysDevice.getName());
            dahuaAlarmVo.setCode(zaSysDevice.getCode());
            dahuaAlarmVo.setIp(zaSysDevice.getIp());

            MqMessage.Facility facility = MqMessage.createFacility(zaSysDevice);
            MqMessage message = MqMessage.createAlarm(zaSysDevice.getId(), dhSdkHandler.getProtocol(), facility, alarmType+"", JSONObject.toJSONString(dahuaAlarmVo));
            dhSdkHandler.consumeMsg(message);
        });

        return R.error(0, "告警成功");
    }

    /**
     * 保存图片
     * @param multipartFile
     * @param dhClientID
     * @param alarmType
     * @return
     */
    private String saveImage(MultipartFile multipartFile, String dhClientID, Integer alarmType){
        if(multipartFile == null || multipartFile.isEmpty()) {
            log.info("大华告警没有图片");
            return null;
        }

        String basePath = ZhianConfig.getProfile();
        File f = new File(basePath+"/dahua");
        if (!f.exists()) {
            f.mkdirs();
        }
        try {
            String strFileName = "/dahua/" + dhClientID + "_" + alarmType + ".jpg";
            multipartFile.transferTo(new File(basePath + strFileName));
            //return  "http://" + IpUtils.getHostIp() +":"+ wsPort + Constants.RESOURCE_PREFIX + strFileName;
            return  Constants.RESOURCE_PREFIX + strFileName;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 接收ICC平台上报的告警消息
     * @return
     */
    @PostMapping("icc")
    public R iccMsg(@RequestBody String msgStr){
        log.info("----接收到[大华ICC平台的]告警消息 : {}",  msgStr);

        IccAlarmMsg alarmMsg = JSONObject.parseObject(msgStr, IccAlarmMsg.class);
        DisruptorUtil.push(()->{
            dhIccHandler.processMsg(alarmMsg);
        });

        return R.error(0, "已接收到消息");
    }
}
