package com.zhian.gateway.third.dahua.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dahuatech.hutool.http.HttpUtil;
import com.dahuatech.hutool.http.Method;
import com.dahuatech.icc.oauth.http.IccTokenResponse;
import com.dahuatech.icc.oauth.model.v202010.GeneralResponse;
import com.dahuatech.icc.oauth.model.v202010.OauthConfigUserPwdInfo;
import com.dahuatech.icc.oauth.utils.HttpUtils;
import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.framework.cache.RedisCache;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import com.zhian.gateway.third.common.constants.AlarmType;
import com.zhian.gateway.third.dahua.DhIccHandler;
import com.zhian.gateway.third.dahua.common.IccDeviceType;
import com.zhian.gateway.third.dahua.vo.*;
import com.zhian.gateway.third.dahua.vo.*;
import com.zhian.gateway.third.video.vo.VideoRecord;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 大华平台对接服务
 */
@Service
@Slf4j
public class DahuaIccService {

    public static final String TOKEN_CACHE = "dh_icc";
    public static final String URL_OperateCamera = "/evo-apigw/admin/API/DMS/Ptz/OperateCamera";
    public static final String URL_OperateDirect = "/evo-apigw/admin/API/DMS/Ptz/OperateDirect";
    public static final String URL_realtime = "/evo-apigw/admin/API/video/stream/realtime";
    public static final String URL_QueryRecords = "/evo-apigw/admin/API/SS/Record/QueryRecords";
    public static final String URL_record = "/evo-apigw/admin/API/video/stream/record";
    public static final String URL_subscribe = "/evo-apigw/evo-event/1.0.0/subscribe/mqinfo";
    public static final String URL_page = "/evo-apigw/evo-brm/1.2.0/device/subsystem/page";
    public static final String URL_call = "/evo-apigw/evo-brm/1.2.0/device/subsystem/page";
    @Autowired
    private RedisCache redisCache;
    @Autowired
    protected IZaSysErrorService zaSysErrorService;
    @Autowired
    protected IZaSysDeviceService deviceService;

    //延迟任务，主要是云台控制使用
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
    OauthConfigUserPwdInfo oauthConfig = null;

    /**
     * 停止
     */
    public void stop(){
        redisCache.deleteObject(TOKEN_CACHE + ":token");
        redisCache.deleteObject(TOKEN_CACHE + ":user");
        redisCache.deleteObject(TOKEN_CACHE + ":time");
    }

    /**
     * 刷新令牌
     */
    public void refreshToken(){
        Long ts =  redisCache.getCacheObject(TOKEN_CACHE + ":time");
        if(ts == null || System.currentTimeMillis() - ts > 60 * 60 * 1000){
            log.error("重新获取令牌");
            stop();
            getToken(oauthConfig);
        }else{
            log.info("当前令牌{}，获取时间: {}", redisCache.getCacheObject(TOKEN_CACHE + ":token"), new Date(ts));
        }
    }

    /**
     * 获取令牌
     * @return
     */
    public synchronized String getToken(OauthConfigUserPwdInfo oauthConfig){
        this.oauthConfig = oauthConfig;
        String token = redisCache.getCacheObject(TOKEN_CACHE+":token");
        if(token != null){
            return token;
        }
        //执行请求
        try {
            IccTokenResponse.IccToken iccToken = HttpUtils.getToken(oauthConfig);
            if(iccToken != null) {
                token = iccToken.getAccess_token();
                redisCache.setCacheObject(TOKEN_CACHE + ":token", token, 100, TimeUnit.MINUTES);
                redisCache.setCacheObject(TOKEN_CACHE + ":user", iccToken.getUserId(), 100, TimeUnit.MINUTES);
                redisCache.setCacheObject(TOKEN_CACHE + ":time", System.currentTimeMillis());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(token == null) {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "获取大华令牌失败", null, JSONObject.toJSONString(oauthConfig));
            }
        }
        return token;
    }
    /**
     * 获取令牌
     * @return
     */
    public Map<String, String> getHeader(){
        String token = getToken(oauthConfig);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);
        headers.put("User-Id", redisCache.getCacheObject(TOKEN_CACHE+":user"));
        return headers;
    }

    /**
     * 订阅告警
     * @param url 回调地址
     * @return
     */
    public boolean subscribe(String url){
        String magic = url.substring(url.indexOf("//")+2);
        magic = magic.substring(0, magic.indexOf("/"));
        magic = magic.replace(':','_');
        IccSubscribeParam.Subsystem subsystem = new IccSubscribeParam.Subsystem(0, magic, magic);
        IccSubscribeParam.SubscribeEvent[] events = new IccSubscribeParam.SubscribeEvent[3];
        events[0] = new IccSubscribeParam.SubscribeEvent("alarm", 1, 2);
        events[1] = new IccSubscribeParam.SubscribeEvent("state", 1, 2);
        events[2] = new IccSubscribeParam.SubscribeEvent("business", 1, 2);
        IccSubscribeParam.SubscribeMonitor[] monitors = new IccSubscribeParam.SubscribeMonitor[1];
        monitors[0] = new IccSubscribeParam.SubscribeMonitor(url, "url", events);
        IccSubscribeParam.SubscribeParam param = new IccSubscribeParam.SubscribeParam(monitors, subsystem);
        IccSubscribeParam data = new IccSubscribeParam(param);
        try{
            IccCommonResponse response = HttpUtils.executeJson(URL_subscribe, data, getHeader(), Method.POST, oauthConfig, IccCommonResponse.class);
            return response != null && response.isSuccess();
        }catch (Exception e){
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "订阅大华ICC平台失败", e.getMessage(), JSONObject.toJSONString(data));
            return false;
        }
    }

    /**
     * 同步摄像机和状态,包括门禁/道闸等设备
     */
    public void syncDevice(DhIccHandler handler) {
        executor.schedule(()->{syncDevicePage(handler);}, 1, TimeUnit.SECONDS);
    }

    /**
     * 同步摄像机和状态
     */
    private void syncDevicePage(DhIccHandler handler) {
        IccCameraResponse response = null;
        JSONObject params = new JSONObject();
        params.put("pageSize", 100);
        try{
            int page = 1;
            while (true){
                params.put("pageNum", page);
                response = HttpUtils.executeJson(URL_page, params, getHeader(), Method.POST, oauthConfig, IccCameraResponse.class);
                if(response.getData().getPageData() == null || response.getData().getPageData().size() == 0){
                    break;
                }

                for(IccCameraResponse.DeviceInfo deviceInfo:response.getData().getPageData()){
                    saveDevice(deviceInfo, handler);
                }
                page++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 保存设备信息
     * @param deviceInfo
     * @param handler
     */
    private void saveDevice(IccCameraResponse.DeviceInfo deviceInfo, DhIccHandler handler){
        ZaSysDevice camera = deviceService.selectZaSysDeviceByCode(deviceInfo.getDeviceCode(), "icc");
        //设备已经存在
        if(camera != null){
            if(deviceInfo.getDeviceCategory() == 1 && StringUtils.isEmpty(camera.getRemark())){
                Optional<IccCameraResponse.UnitsInfo> unitsInfo = deviceInfo.getUnits().stream().filter(u->u.getUnitType() == 1).findFirst();
                if(unitsInfo.isPresent()){
                    String channels = unitsInfo.get().getChannels().stream().map(IccCameraResponse.CameraChannel::getChannelCode).collect(Collectors.joining(","));
                    camera.setRemark(channels);
                    deviceService.updateZaSysDevice(camera);
                }
            }
            if(!camera.getOnline().equalsIgnoreCase(deviceInfo.getIsOnline())){
                camera.setOnline(deviceInfo.getIsOnline());
                deviceService.updateZaSysDevice(camera);
                MqMessage.Facility facility = MqMessage.createFacility(camera);
                MqMessage mqMessage = null;
                if(deviceInfo.getIsOnline().equalsIgnoreCase("1")){
                    mqMessage = MqMessage.createAlarm(camera.getId(), handler.getProtocol(), facility, AlarmType.ONLINE.getCode(), JSONObject.toJSONString(camera));
                }else{
                    mqMessage = MqMessage.createAlarm(camera.getId(), handler.getProtocol(), facility, AlarmType.OFFLINE.getCode(), JSONObject.toJSONString(camera));
                }
                handler.consumeMsg(mqMessage);
            }
            return;
        }
        camera = new ZaSysDevice();

        //设备不存在，先解析，再统一处理
        camera.setName(deviceInfo.getDeviceName());
        camera.setCode(deviceInfo.getDeviceCode());
        camera.setIp(deviceInfo.getDeviceIp());
        camera.setNet("icc");
        camera.setWireless("N");
        camera.setOnline(deviceInfo.getIsOnline());
        camera.setPfCode(handler.getPlatform());
        camera.setModel(deviceInfo.getDeviceModel());

        if(deviceInfo.getDeviceCategory() == 1){
            //摄像机
            if(deviceInfo.getUnits() == null || deviceInfo.getUnits().size() == 0){
                log.error("摄像机{} - {}没有单元信息，无法添加", deviceInfo.getDeviceIp(), deviceInfo.getDeviceName());
                return;
            }
            Optional<IccCameraResponse.UnitsInfo> unitsInfo = deviceInfo.getUnits().stream().filter(u->u.getUnitType() == 1).findFirst();
            if(!unitsInfo.isPresent()){
                log.error("摄像机{} - {}没有视频通道，暂不添加", deviceInfo.getDeviceIp(), deviceInfo.getDeviceName());
                return;
            }
            camera.setType(IccDeviceType.CAMERA);
            String channels = unitsInfo.get().getChannels().stream().map(IccCameraResponse.CameraChannel::getChannelCode).collect(Collectors.joining(","));
            camera.setRemark(channels);
        }else if(deviceInfo.getDeviceCategory() == 5){
            //卡口
            camera.setType(IccDeviceType.ANPR);
        }else if(deviceInfo.getDeviceCategory() == 8){
            //门禁
            if(deviceInfo.getDeviceType().equalsIgnoreCase("15") || deviceInfo.getDeviceType().equalsIgnoreCase("16")) {
                camera.setType(IccDeviceType.ACSM);
            }else if(deviceInfo.getDeviceType().equalsIgnoreCase("21")){
                camera.setType(IccDeviceType.ACSC);
            }else{
                camera.setType(IccDeviceType.ACS);
            }
        }else if(deviceInfo.getDeviceCategory() == 21){
            //可视化对讲
            camera.setType(IccDeviceType.VI);
        }else if(deviceInfo.getDeviceCategory() == 36){
            //道闸
            camera.setType(IccDeviceType.TSSG);
        }else{
            log.error("暂时不需要接入的设备类型： {} - {} - {}", deviceInfo.getDeviceCategory(), deviceInfo.getDeviceType(), deviceInfo.getDeviceName());
            //其它设备，暂不接入
        }

        //保存入库并推送
        if(camera != null){
            deviceService.insertZaSysDevice(camera);

            MqMessage.Facility facility = MqMessage.createFacility(camera);
            MqMessage message = MqMessage.createDevice(camera.getId(), handler.getProtocol(), facility, JSONObject.toJSONString(camera));
            message.setEventType(MqMessage.DEVICE_ADD);
            handler.consumeMsg(message);
        }
    }

    /**
     * 查询录像，目前只查询主码流
     * @param camera
     * @param day
     * @return
     */
    public R queryRecords(ZaSysDevice camera, String day){
        String chns[] = camera.getRemark().split(",");
        IccDataParam param = IccDataParam.create("channelId", chns[0]);
        param.set("recordSource", "1").set("streamType", "0").set("recordType", "0");
        param.set("startTime", DateUtils.parseDate(day + " 00:00:00").getTime()/1000);
        param.set("endTime", DateUtils.parseDate(day + " 23:59:59").getTime()/1000);
        try{
            IccCommonResponse response = HttpUtils.executeJson(URL_QueryRecords, param, getHeader(), Method.POST, oauthConfig, IccCommonResponse.class);

            if(response == null || !response.isSuccess()){
                return R.error("没有找到录像");
            }
            JSONArray records = response.getData().getJSONArray("records");
            if(records == null || records.size() == 0){
                return R.error("没有找到录像");
            }
            List<VideoRecord> list = new ArrayList<>();
            for (int i = 0; i < records.size(); i++) {
                JSONObject record = records.getJSONObject(i);
                VideoRecord videoRecord = new VideoRecord();
                videoRecord.setStartTime(new Date(record.getLong("startTime")*1000));
                videoRecord.setEndTime(new Date(record.getLong("endTime")*1000));
                videoRecord.setName(record.getString("recordName"));
                String name = day+"_"+DateUtils.parseDateToStr("HH:mm:ss", videoRecord.getStartTime()) + "_" + DateUtils.parseDateToStr("HH:mm:ss", videoRecord.getEndTime());
                name += "_"+record.getString("recordSource")+"_"+record.getString("streamType");
                videoRecord.setName(name);
                list.add(videoRecord);
            }

            return R.success(list);
        }catch (Exception e){
            e.printStackTrace();
            return R.error("调用接口失败");
        }
    }

    /**
     * 查询指定的录像流地址
     * @param camera
     * @param name
     * @return
     */
    public R queryRecordStream(ZaSysDevice camera, String name){
        String chns[] = camera.getRemark().split(",");
        String[] ps = name.split("_");
        IccDataParam param = IccDataParam.create("channelId", chns[0]).set("type", "hls");
        param.set("beginTime", ps[0]+" " + ps[1]);
        param.set("endTime",ps[0]+" " + ps[2]);
        param.set("recordSource", ps[3]).set("streamType", ps[4]).set("recordType", "1");

        try{
            String urls = "";
            for(String chn:chns){
                param.set("channelId", chn);
                IccCommonResponse response = HttpUtils.executeJson(URL_record, param, getHeader(), Method.POST, oauthConfig, IccCommonResponse.class);

                if(response == null || !response.isSuccess()){
                    break;
                }
                urls += "," +response.getData().getString("url")+"?token="+getToken(oauthConfig);
            }
            if(StringUtils.isEmpty(urls)){
                return R.error("未找到回放流");
            }else {
                return R.success(urls.substring(1), "dh");
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error("调用接口失败");
        }
    }

    /**
     * 服务器地址
     * @param port
     * @return
     */
    private String getServer(Integer port){
        return "https://"+oauthConfig.getHttpConfigInfo().getHost(); //+":"+(port == null ? oauthConfig.getHttpConfigInfo().getHttpPort() : port);
    }

    /**
     * 远程抓图
     * @param camera 摄像机
     * @param chn  通道号
     * @return
     */
    public String snapPicture(ZaSysDevice camera, int chn) {
        IccSnapParam param = IccSnapParam.create(camera.getCode(), chn);
        try{
            IccSnapResponse response = HttpUtils.executeJson("/evo-apigw/admin/API/EVO/invoke/DMS", param, getHeader(), Method.POST, oauthConfig, IccSnapResponse.class);

            if(response == null || !response.isSuccess()){
                return null;
            }

            log.debug("截图{}->{}", camera.getCode(), response.getImg());
            return getServer(null)+"/evo-apigw/evo-oss/" + response.getImg();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 响应图片内容
     * @param picUrl
     * @return
     * @throws IOException
     */
    public R responseImage(String picUrl) {
        R ret = R.error();
        try {
            picUrl += "?token="+getToken(oauthConfig);
            log.info("下载图片："+picUrl);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HttpUtil.download(picUrl, os, true);
            ret = R.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(os.toByteArray()));
            // 完毕，关闭所有链接
            os.close();
            //is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 下载停车场/卡口图片
     * @param picUrl
     * @return
     */
    public String downloadImg(String picUrl, String deviceCode){
        String basePath = ZhianConfig.getProfile();
        File f = new File(basePath+"/dahua");
        if (!f.exists()) {
            f.mkdirs();
        }
        try {
            if(picUrl.toLowerCase().startsWith("http")){
                picUrl = picUrl+"?token="+getToken(oauthConfig);
            }else if(picUrl.toLowerCase().contains("fileUrl")){
                picUrl = getServer(null)+"/evo-apigw"+picUrl+"&token="+getToken(oauthConfig);
            }else if(picUrl.toLowerCase().contains("oss")){
                picUrl  = picUrl.substring(4);
                picUrl = picUrl.substring(picUrl.indexOf('/'));
                picUrl = getServer(null)+"/evo-apigw/evo-oss"+picUrl+"?token="+getToken(oauthConfig);
            }else{
                picUrl = getServer(9480)+picUrl.substring(picUrl.indexOf("/d/"))+"?token="+getToken(oauthConfig);
            }
            String strFileName = "/dahua/" + deviceCode + "_" + System.currentTimeMillis() + ".jpg";
            log.debug("下载icc图片 {} -> {}", picUrl, basePath + strFileName);
            HttpUtil.download(picUrl,  new FileOutputStream(basePath + strFileName), true);
            return  Constants.RESOURCE_PREFIX + strFileName;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载图片
     * @param picUrl
     * @return
     */
    public String downloadImg(String picUrl, String deviceCode, Integer alarmType){
        String basePath = ZhianConfig.getProfile();
        File f = new File(basePath+"/dahua");
        if (!f.exists()) {
            f.mkdirs();
        }
        try {
            if(!picUrl.toLowerCase().startsWith("http")){
                picUrl = getServer(null)+"/evo-apigw/evo-oss/"+picUrl+"?token="+getToken(oauthConfig);
            }
            String strFileName = "/dahua/" + deviceCode + "_" + alarmType + ".jpg";
            log.debug("下载icc图片 {} -> {}", picUrl, basePath + strFileName);
            HttpUtil.download(picUrl,  new FileOutputStream(basePath + strFileName), true);
            return  Constants.RESOURCE_PREFIX + strFileName;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 实时拉流
     * @param camera
     * @return
     */
    public R realPlay(ZaSysDevice camera){
        IccDataParam param = IccDataParam.create("channelId", camera.getCode());
        param.set("streamType", "1").set("type", "ws_flv");
        try{
            String urls = "";
            String[] chns = camera.getRemark().split(",");
            for(String chn:chns){
                param.set("channelId", chn);
                IccCommonResponse response = HttpUtils.executeJson(URL_realtime, param, getHeader(), Method.POST, oauthConfig, IccCommonResponse.class);

                if(response == null || !response.isSuccess()){
                    if(response.getCode().equalsIgnoreCase("27001007")){
                        refreshToken();
                    }
                    log.error("拉流时响应: {}", response);
                    break;
                }
                urls += "," +response.getData().getString("url")+"?token="+getToken(oauthConfig);
            }
            return urls.length() == 0 ? R.error("拉流失败") : R.success(urls.substring(1), "dh");
        }catch (Exception e){
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "拉取视频流失败", e.getMessage(), JSONObject.toJSONString(param));
            return R.error("拉取视频流失败");
        }
    }



    /**
     * 云台控制
     * @param camera
     * @param cmdValue
     */
    public void ptz(ZaSysDevice camera, String cmdValue){

        String url = "/evo-apigw/admin/API/DMS/Ptz/OperateDirect";
        String[] vs = cmdValue.split(":");
        int speed2 = (vs.length > 1) ? Integer.parseInt(vs[1]) : 1;
        int speed1 = 0; //上左|上右|下左|下右时，才需要这个速度参数
        String direct = null;
        switch (vs[0].toLowerCase()){
            case "left":
                direct = "3";
                break;
            case "left_up":
                direct = "5";
                speed1=speed2;
                break;
            case "left_down":
                direct = "6";
                speed1=speed2;
                break;
            case "right":
                direct = "4";
                break;
            case "right_up":
                direct = "7";
                speed1=speed2;
                break;
            case "right_down":
                direct = "8";
                speed1=speed2;
                break;
            case "up":
                direct = "1";
                break;
            case "down":
                direct = "2";
                break;
            case "in":
                direct = "1";
                url = "/evo-apigw/admin/API/DMS/Ptz/OperateCamera";
                break;
            case "out":
                direct = "2";
                url = "/evo-apigw/admin/API/DMS/Ptz/OperateCamera";
                break;
            case "stop":
                break;
            default:
                break;
        }
        if(direct == null){
            return;
        }

        String chns[] = camera.getRemark().split(",");
        IccDataParam param = IccDataParam.create("channelId", chns[0]);
        if(URL_OperateCamera.equalsIgnoreCase(url)){
            param.set("operateType", "1").set("direct", direct).set("step", speed2);
        }else{
            param.set("direct", direct).set("stepX", speed1).set("stepY", speed2);
        }

        //开始
        GeneralResponse response = postJson(url, param.set("command", "1"));
        if(response == null || !response.isSuccess()){
            return;
        }

        //设置延迟停止
        final String nextUrl = url;
        String ptzDelay = DhIccHandler.zaSysPlatform.getConfigStr("ptzDelay", "3");
        executor.schedule(()-> {
            postJson(nextUrl, param.set("command", "0"));
        },Integer.parseInt(ptzDelay), TimeUnit.SECONDS);
    }

    /**
     * 发起POST请求
     * @param url
     * @param data
     * @return
     */
    public GeneralResponse postJson(String url, Object data){
        try{
            GeneralResponse response = HttpUtils.executeJson(url, data, getHeader(), Method.POST, oauthConfig, GeneralResponse.class);
            log.info("icc response {} -> {}", url, response);
            return response;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
