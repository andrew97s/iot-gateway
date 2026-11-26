package com.zhian.gateway.third.dahua;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dahuatech.icc.oauth.model.v202010.OauthConfigUserPwdInfo;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.ip.IpUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.AlarmType;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.common.vo.CarIoMsg;
import com.zhian.gateway.third.common.vo.DoorIoMsg;
import com.zhian.gateway.third.common.vo.ViCallRecord;
import com.zhian.gateway.third.dahua.service.DahuaIccService;
import com.zhian.gateway.third.dahua.vo.DahuaAlarmVo;
import com.zhian.gateway.third.dahua.vo.IccAlarmMsg;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

/**
 * 对接大华ICC平台
 * ip  是大华平台的地址
 * port 是大华平台API的端口
 * {
 * "username":"openapi账号"，
 * "password":"openapi密码"，
 * "client_id":"凭证key"，
 * "client_secret":"凭证密钥"
 * }
 */
@Component("dhIccHandler")
@Slf4j
public class DhIccHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "dhIcc";
    public static final String PROTOCOL_NAME = "dh";
    public static ZaSysPlatform zaSysPlatform;
    OauthConfigUserPwdInfo oauthConfig = null;
    private static Boolean running = false;

    @Autowired
    private DahuaIccService iccService;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        log.info("启动大华ICC对接插件：{}", zaSysPlatform.getCode());
        DhIccHandler.zaSysPlatform = zaSysPlatform;
        if (StringUtils.isEmpty(zaSysPlatform.getIp()) || StringUtils.isEmpty(zaSysPlatform.getConfigStr("clientId"))) {
            log.error("大华ICC平台配置参数不全，无法启动: {}", zaSysPlatform);
            return false;
        }
        oauthConfig = new OauthConfigUserPwdInfo(zaSysPlatform.getIp(), zaSysPlatform.getConfigStr("clientId"), zaSysPlatform.getConfigStr("clientSecret"), zaSysPlatform.getConfigStr("username"), zaSysPlatform.getConfigStr("password"), false, zaSysPlatform.getPort().toString());
        oauthConfig.getHttpConfigInfo().setReadTimeout(-1l);//设置读取超时时间，单位毫秒，默认-1
        oauthConfig.getHttpConfigInfo().setConnectionTimeout(-1l);//设置连接超时，单位毫秒，默认-1

        String token = iccService.getToken(oauthConfig);
        if (token != null) {
            log.info("获取大华ICC对接令牌：{}", token);

            //订阅告警事件
            String url = zaSysPlatform.getConfigStr("callback");
            if (StringUtils.isEmpty(url)) {
                url = "http://" + IpUtils.getHostIp() + ":9200/dahua/icc";
            }
            if (iccService.subscribe(url)) {
                log.info("订阅大华ICC告警事件成功: {}", url);
            } else {
                log.error("订阅大华ICC告警事件失败: {}", url);
            }

            //同步一次设备
            iccService.syncDevice(this);
            running = true;
        }
        return running;
    }

    @Override
    public boolean stop() {
        log.info("停止大华{}的相关操作", zaSysPlatform.getCode());
        iccService.stop();
        running = false;
        return true;
    }


    /**
     * 确认第三方对接服务是否正常
     *
     * @return
     */
    @Override
    public boolean isAlive() {
//        //判断设备是否在线
//        iccService.refreshToken();
//        iccService.syncDevice(this);
        return running;
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }


    /**
     * 反向控制
     *
     * @param controlVo 控制对象
     * @return R
     */
    @Override
    public R doControl(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return R.error("设备信息不存在");
        }

        //流地址自动拼接，默认ws协议,支持参数请求http协议
        if (ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())) {
            return iccService.realPlay(camera);
        } else if (ControlVo.CMD_PLAY_BACK.equalsIgnoreCase(controlVo.getCommand())) {
            return iccService.queryRecordStream(camera, controlVo.getValue());
        } else if (ControlVo.CMD_RECORDS.equalsIgnoreCase(controlVo.getCommand())) {
            return iccService.queryRecords(camera, controlVo.getValue());
        } else if (ControlVo.CMD_SNAP.equalsIgnoreCase(controlVo.getCommand())) {
            String url = iccService.snapPicture(camera, 0);
            if (url != null) {
                return iccService.responseImage(url);
            } else {
                return R.error("读取图片失败");
            }
        } else if (ControlVo.CMD_PTZ.equalsIgnoreCase(controlVo.getCommand())) {
            //云台控制
            iccService.ptz(camera, controlVo.getValue());
            return R.success();
        }
        return R.error("暂不支持的控制");
    }

    /**
     * 处理接收到的消息,主要是发现新设备/设备注册/智能告警事件
     *
     * @param msgObj
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj) {
        if (!isAlive()) {
            log.error("大华{}插件暂时停止", zaSysPlatform.getCode());
            return null;
        }

        ProcessInfo info = null;
        IccAlarmMsg alarmMsg = (msgObj instanceof IccAlarmMsg) ? (IccAlarmMsg) msgObj : JSONObject.parseObject(msgObj.toString(), IccAlarmMsg.class);
        if (alarmMsg.getCategory() == null) {
            log.error("暂不支持的消息:", msgObj);
        } else if (alarmMsg.getCategory().equalsIgnoreCase("business")) {
            info = processBusiness(alarmMsg);
        } else if (alarmMsg.getCategory().equalsIgnoreCase("alarm")) {
            info = processAlarm(alarmMsg);
        } else if (alarmMsg.getCategory().equalsIgnoreCase("state")) {
            log.warn("state info: {}", msgObj);
        } else {
            log.error("暂不支持的消息大类:", alarmMsg.getCategory());
        }

        return info;
    }

    /**
     * 处理业务事件
     *
     * @param alarmMsg
     */
    public ProcessInfo processBusiness(IccAlarmMsg alarmMsg) {
        ProcessInfo processInfo = null;
        String method = alarmMsg.getMethod();
        if (method.startsWith("device.")) {
            IccAlarmMsg.Info info = JSONObject.parseObject(alarmMsg.getInfo(), IccAlarmMsg.Info.class);
            if (alarmMsg.getMethod().equalsIgnoreCase("device.delete")) {
                ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(info.getDeviceCode(), "icc");
                if (zaSysDevice == null) {
                    log.error("大华摄像机{}未注册，不需要同步删除", info.getDeviceCode());
                } else {
                    deviceService.deleteZaSysDeviceById(zaSysDevice.getId());
                    processInfo = ProcessInfo.newDevice(
                            zaSysDevice,
                            JSON.toJSONString(alarmMsg),
                            Collections.singletonList(
                                    DeviceUtil.genMessage(DeviceUpdReq.newDelReq(zaSysDevice,null))
                            )
                    );
                }
            } else {
                log.info("摄像机设备变更{}，将进行一次设备同步", alarmMsg.getMethod());
                iccService.syncDevice(this);
            }
        } else if (StrUtil.equals(method, "callRecord.msg")) {
            //对讲
            ViCallRecord callRecord = JSONObject.parseObject(alarmMsg.getInfo(), ViCallRecord.class);
            JSONObject info = JSONObject.parseObject(alarmMsg.getInfo());

            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(callRecord.getCalleeCode(), "icc");
            if (zaSysDevice == null) {
                log.error("大华设备{}未在ICC平台注册", callRecord.getCalleeCode());
                return null;
            }

            if (info.containsKey("time")) {
                callRecord.setBeginTime(new Date(info.getLong("time")));
            }
            if (info.containsKey("endTime")) {
                callRecord.setEndTime(new Date(info.getLong("endTime")));
            }

            processInfo = ProcessInfo.newBusiness(
                    zaSysDevice,
                    JSON.toJSONString(alarmMsg),
                    Collections.singletonList(
                            MqMessage.createBusiness(
                                    zaSysDevice,
                                    "vi",
                                    PROTOCOL_NAME,
                                    JSONObject.toJSONString(callRecord),
                                    null
                            )
                    ));
        } else if (StrUtil.equals(method, "car.access")) {
            CarIoMsg carIoMsg = new CarIoMsg();
            String deviceCode = null;
            JSONObject info = JSONObject.parseObject(alarmMsg.getInfo());
            carIoMsg.setMsgId(alarmMsg.getId());
            if (info.containsKey("exitTime") && info.get("exitTime") != null) {
                deviceCode = info.getString("exitSluiceDevChnid");
                carIoMsg.setType("2");
                carIoMsg.setGateName(info.getString("exitSluiceDevChnname"));
                carIoMsg.setPlate(info.getString("exitCarNum"));
                carIoMsg.setIoTime(info.getDate("exitTime"));
                carIoMsg.setImgFile(iccService.downloadImg(info.getString("realCapturePicPathExit"), deviceCode));
            } else {
                deviceCode = info.getString("enterSluiceDevChnid");
                carIoMsg.setType("1");
                carIoMsg.setGateName(info.getString("enterSluiceDevChnname"));
                carIoMsg.setPlate(info.getString("carNum"));
                carIoMsg.setIoTime(info.getDate("enterTime"));
                carIoMsg.setImgFile(iccService.downloadImg(info.getString("realCapturePicPathEnter"), info.getString(deviceCode)));
            }

            ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(deviceCode, "icc");
            if (zaSysDevice == null) {
                log.error("大华设备{}未在ICC平台注册", deviceCode);
                return null;
            }

            processInfo = ProcessInfo.newBusiness(
                    zaSysDevice,
                    JSON.toJSONString(alarmMsg),
                    Collections.singletonList(
                            MqMessage.createBusiness(
                                    zaSysDevice,
                                    "gate",
                                    PROTOCOL_NAME,
                                    JSONObject.toJSONString(carIoMsg),
                                    null
                            )
                    ));
        } else {
            //cardRecord.offline	离线补采刷卡记录	门禁管理
            //delivery-state.change	门禁授权结果推送事件	门禁管理
            //visitor.record	访客行踪推送	访客管理	访客通行记录：门禁、可视对讲、人员识别、停车场
            //visitor.status	访客状态变更	访客管理	访客预约（新增）、登记、签离、取消预约
            //car.capture	过车事件	停车场管理	抓拍记录和进出场区记录，道闸设备触发
            log.error("暂不支持大华业务事件：{}", alarmMsg.getMethod());
        }
        return processInfo;
    }


    /**
     * 处理告警消息
     *
     * @param alarmMsg
     */
    private ProcessInfo processAlarm(IccAlarmMsg alarmMsg) {
        IccAlarmMsg.Info alarmInfo = JSONObject.parseObject(alarmMsg.getInfo(), IccAlarmMsg.Info.class);
        ZaSysDevice zaSysDevice = deviceService.selectZaSysDeviceByCode(alarmInfo.getDeviceCode(), "icc");
        if (zaSysDevice == null) {
            log.error("大华设备{}未在ICC平台注册", alarmInfo.getDeviceCode());
            return null;
        }

        //门禁模块，如果是通行事件，按业务数据单独处理
        if (alarmInfo.getUnitType() == 7) {
            JSONObject ext = JSONObject.parseObject(alarmInfo.getExtend());
            if (ext.containsKey("openResult") && ext.getInteger("openResult") == 1) {
                DoorIoMsg doorIoMsg = new DoorIoMsg();
                doorIoMsg.setDoorName(ext.getString("acsChannelName"));
                doorIoMsg.setMsgId(ext.getString("id"));
                doorIoMsg.setIdnum(ext.getString("paperNumber"));
                doorIoMsg.setIoTime(ext.getDate("swingTime"));
                doorIoMsg.setPersonNo(ext.getString("personCode"));
                doorIoMsg.setName(ext.getString("personName"));
                doorIoMsg.setUserType(ext.getInteger("vRecordFlag") == 0 ? "0" : "1");
                doorIoMsg.setImgFile(iccService.downloadImg(ext.getString("recordImage1"), alarmInfo.getDeviceCode()));
                if (ext.getString("openTypeStr").contains("开门")) {
                    doorIoMsg.setType("1");
                } else {
                    doorIoMsg.setType("2");
                }

                MqMessage message = MqMessage.createBusiness(zaSysDevice, "door", PROTOCOL_NAME, JSONObject.toJSONString(doorIoMsg), null);
                return ProcessInfo.newBusiness(
                        zaSysDevice,
                        JSON.toJSONString(alarmMsg) ,
                        Collections.singletonList(message)
                );
            }
        }

        DahuaAlarmVo alarmVo = new DahuaAlarmVo();
        alarmVo.setCode(zaSysDevice.getCode());
        alarmVo.setName(zaSysDevice.getName());
        alarmVo.setIp(zaSysDevice.getIp());
        if (alarmInfo.getStatus() == null) {
            //告警
            alarmVo.setAlarmType(alarmInfo.getAlarmType());
            alarmVo.setAlarmTime(alarmInfo.getAlarmDate() * 1000);
            //如果有图片就先下载
            if (StringUtils.isNotEmpty(alarmInfo.getAlarmPicture())) {
                alarmVo.setAlarmImage(iccService.downloadImg(alarmInfo.getAlarmPicture(), zaSysDevice.getCode(), alarmVo.getAlarmType()));
            }
            alarmVo.setAlarmStat(alarmInfo.getAlarmStat());
        } else {
            //状态事件
            if (alarmInfo.getStatus() == 1) {
                //上线
                alarmVo.setAlarmType(Integer.parseInt(AlarmType.ONLINE.getCode()));
                alarmVo.setComment("设备上线");
                alarmVo.setAlarmStat(2);
            } else if (alarmInfo.getStatus() == 0) {
                //离线
                alarmVo.setAlarmType(Integer.parseInt(AlarmType.OFFLINE.getCode()));
                alarmVo.setComment("设备离线");
                alarmVo.setAlarmStat(1);
            } else {
                log.error("不支持告警消息: {}", alarmMsg);
            }
            alarmVo.setAlarmTime(alarmInfo.getUpdateTime().getTime());
        }
        MqMessage.Facility facility = MqMessage.createFacility(zaSysDevice);
        facility.setNet(null); //SDK接入暂时没有net字段
        MqMessage message = MqMessage.createAlarm(zaSysDevice.getId(), PROTOCOL_NAME, facility, alarmVo.getAlarmType() + "", JSONObject.toJSONString(alarmVo));
        return ProcessInfo.newAlarm(zaSysDevice, JSON.toJSONString(alarmMsg) , Collections.singletonList(message));
    }

}
