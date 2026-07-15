package com.zhian.gateway.third.video.jinzhi;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.video.jinzhi.vo.JinzhiRecord;
import com.zhian.gateway.third.video.vo.VideoRecord;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 金智视频网关对接
 */
@Component
@Slf4j
public class JinZhiHandler extends BasePlatformHandler {
    public static final String PLATFORM_NAME = "jinzhi";
    public static final String PROTOCOL_NAME = "jz";
    private static ZaSysPlatform zaSysPlatform;
    private static Boolean running = false;
    private static Map<String, Session> sessionMap = new HashMap<>();

    //延迟任务，主要是云台控制使用
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        log.info("启动视频网关");
        JinZhiHandler.zaSysPlatform = zaSysPlatform;
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        closeAll();
        log.info("将忽略金智视频网关平台推送过来的数据");
        running = false;
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     * @return
     */
    @Override
    public boolean isAlive(){
//        ZaSysDevice dc = new ZaSysDevice();
//        dc.setType(DeviceType.VAG);
//        dc.setModel(JinzhiConst.MODEL_NAME);
//        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
//        for(ZaSysDevice gateway:list){
//            //原网关不在线，现在上线了
//            if(sessionMap.containsKey(gateway.getCode()) && "0".equalsIgnoreCase(gateway.getOnline())){
//                gateway.setOnline("1");
//                deviceService.updateZaSysDevice(gateway);
//                // TODO
//                // pushState(gateway, AlarmType.ONLINE.getCode());
//            }else if(!sessionMap.containsKey(gateway.getCode()) && "1".equalsIgnoreCase(gateway.getOnline())){
//                //原网关在线，现在不在线了
//                gateway.setOnline("0");
//                deviceService.updateZaSysDevice(gateway);
//                // TODO
//                // pushState(gateway, AlarmType.OFFLINE.getCode());
//            }
//            Session session = sessionMap.get(gateway.getCode());
//            if(session != null){
//                JinzhiWebSocket.sendMsg(session, JinzhiWebSocket.DEVICE_LIST_GET_REQ);
//            }
//        }
//        //不再主动拉设备，等待从Websocket推送
//        //
        return running;
    }

    /**
     * 关闭所有连接
     */
    public void closeAll(){
        CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, "服务器暂时关闭");
        Set<String> ss = new HashSet<>();
        ss.addAll(sessionMap.keySet());
        for(String k:ss){
            if(!sessionMap.containsKey(k)){
                continue;
            }
            Session session = sessionMap.get(k);
            log.info("主动关闭视频网关的ws连接 {}", session.getId());
            try{
                session.close(closeReason);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        sessionMap.clear();
    }

    /**
     * 网关上线
     * @param gateway
     * @Param session WS
     */
    public void addGateway(ZaSysDevice gateway, Session session){
        sessionMap.put(gateway.getCode(), session);

        log.info("网关 {}-{}-{} 已上线", gateway.getModel(), gateway.getIp(), gateway.getCode());
    }

    /**
     * 网关下线
     * @param gateway
     */
    public void removeGateway(ZaSysDevice gateway){
        sessionMap.remove(gateway.getCode());
        log.info("网关 {}-{}-{} 已下线", gateway.getModel(), gateway.getIp(), gateway.getCode());
    }


    /**
     * 摄像机截图
     * @param gateway
     * @param camera
     */
    private byte[] snap(ZaSysDevice gateway, ZaSysDevice camera){

        String api = "http://" + gateway.getIp() + ":8080/agapi/snap?deviceID="+camera.getCode()+"&sec=0&time="+System.currentTimeMillis();
        //获取任务列表
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "LX_TOKEN=123456789");
        byte[] result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), byte[].class).getBody();
        return result;
    }


    /**
     * 登录网关，获取令牌
     * @param netDevice
     * @return
     */
    private String getToken(ZaSysDevice netDevice){
        return "123456789";
        /*
        String token = null; // 不缓存token cache.getCacheObject(CACHE_MAP+netDevice.getCode());
        if(token != null){
            return token;
        }

        JSONObject rs = JSONObject.parseObject(netDevice.getRemark());
        String api = "http://" + rs.getString("ip")+ ":" + rs.getString("port")+"/api/v1/login?";
        api +="username=" + rs.getString("username") + "&password=" + Md5Utils.hash(rs.getString("password")) + "&url_token_only=true";
        String str = restTemplate.getForObject(api, String.class);
        LiveQingResult result = JSONObject.parseObject(str, LiveQingResult.class);
        if (result == null || result.getLiveQing() == null) {
            log.error("登录视频网关{} 失败", netDevice.getName());
            return null;
        }
        token = result.getLiveQing().getBody().get("URLToken");
        cache.setCacheObject(CACHE_MAP+netDevice.getCode(), token, Integer.parseInt(result.getLiveQing().getBody().get("TokenTimeout"))-3600, TimeUnit.SECONDS);
        return token;
        */
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
     * 反向控制,支持云台控制和拉视频流
     * @param controlVo
     * @return
     */
    @Override
    public AjaxResult doControl(ControlVo controlVo){
        ZaSysDevice camera = controlVo.getDevice();
        if(camera == null){
            return AjaxResult.error("设备信息不存在");
        }

        ZaSysDevice netDevice = deviceService.selectZaSysDeviceByCode(camera.getNet(), null);
        if(netDevice == null){
            return AjaxResult.error("未找到视频网关");
        }

        JSONObject ns = JSONObject.parseObject(netDevice.getRemark());
        JSONObject cs = JSONObject.parseObject(camera.getRemark());
        //流地址自动拼接，默认ws协议,支持参数请求http协议
        if(ControlVo.CMD_STREAM.equalsIgnoreCase(controlVo.getCommand())){
            String protocol = StringUtils.isEmpty(controlVo.getValue()) ? "ws" : controlVo.getValue();
            if(cs.containsKey("channelCount") && cs.getInteger("channelCount") > 1){
                return AjaxResult.success(protocol + "://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + "-0.live.flv"+","+protocol + "://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + "-1.live.flv");
            }else {
                return AjaxResult.success(protocol + "://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + ".live.flv");
            }
        }else if(ControlVo.CMD_PLAY_BACK.equalsIgnoreCase(controlVo.getCommand())){
            if(cs.containsKey("type") && JinzhiConst.TYPE_GB.equalsIgnoreCase(cs.getString("type"))){
                //国标，回放参数是起止时间段
                return AjaxResult.success("http://" + netDevice.getIp()+":8083/rtp/"+camera.getCode()+"_"+controlVo.getValue()+".live.flv");
            }else if(cs.containsKey("type") && JinzhiConst.TYPE_DHSDKS.equalsIgnoreCase(cs.getString("type"))){
                //回放参数是起止时间段
                if(cs.containsKey("channelCount") && cs.getInteger("channelCount") > 1) {
                    return AjaxResult.success("http://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + "-0_" + controlVo.getValue() + ".live.flv," + "http://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + "-1_" + controlVo.getValue() + ".live.flv");
                }else{
                    return AjaxResult.success("http://" + netDevice.getIp() + ":8083/rtp/" + camera.getCode() + "_" + controlVo.getValue() + ".live.flv");
                }
            }else{
                //直连不支持回放
                return AjaxResult.error("直连摄像机不支持回放");
            }
        }else if(ControlVo.CMD_RECORDS.equalsIgnoreCase(controlVo.getCommand())){
            if(cs.containsKey("type") && JinzhiConst.TYPE_GB.equalsIgnoreCase(cs.getString("type"))){
                //国标，参数是日期 2024-08-13

                String api = "http://" + netDevice.getIp() + ":8080/agapi/device/recordfind?deviceID="+camera.getCode()+"&findTimeDay="+controlVo.getValue();
                //获取任务列表
                HttpHeaders headers = new HttpHeaders();
                headers.set("Cookie", "LX_TOKEN=123456789");
                JinzhiRecord result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), JinzhiRecord.class).getBody();
                if(!result.success() || result.getList() == null || result.getList().length == 0){
                    return AjaxResult.error("未查询到录像");
                }
                for(VideoRecord videoRecord: result.getList()){
                    recordName(videoRecord);
                }
                return AjaxResult.success(result.getList());
            }else if(cs.containsKey("type") && JinzhiConst.TYPE_DHSDKS.equalsIgnoreCase(cs.getString("type"))){
                //大华SDK，参数是日期 2024-08-13
                String api = "http://" + netDevice.getIp() + ":8080/agapi/device/recordfind?deviceID="+camera.getCode()+"&findTimeDay="+controlVo.getValue();
                //获取任务列表
                HttpHeaders headers = new HttpHeaders();
                headers.set("Cookie", "LX_TOKEN=123456789");
                JinzhiRecord result = restTemplate.exchange(api, HttpMethod.GET, new HttpEntity(headers), JinzhiRecord.class).getBody();
                if(!result.success() || result.getList() == null || result.getList().length == 0){
                    return AjaxResult.error("未查询到录像");
                }
                for(VideoRecord videoRecord: result.getList()){
                    recordName(videoRecord);
                }
                log.info("records for {} @{}: {}", camera.getCode(), controlVo.getValue(), JSONObject.toJSONString(result));
                return AjaxResult.success(result.getList());
            }else{
                //直连不支持回放
                return AjaxResult.error("直连摄像机不支持回放");
            }
        }

        //截图和云台控制,自己实现
        try {
            if (ControlVo.CMD_SNAP.equalsIgnoreCase(controlVo.getCommand())) {
                /*
                String rtsp  = cs.getString("streamUrl");
                if(StringUtils.isEmpty(rtsp)){
                    log.error("截图 {} - {} 时未找到视频流", camera.getNet(), camera.getCode());
                    return AjaxResult.error("未找到视频流");
                }

                String path = VideoUtil.ffmpeg(rtsp);
                if(StringUtils.isEmpty(path)){
                    log.error("FFMPEG截图 {} - {} 失败", camera.getNet(), camera.getCode());
                    return AjaxResult.error("截图失败");
                }

                File imgFile = new File(ZhianConfig.getUploadPath()+path);
                if(!imgFile.exists()){
                    log.error("未找到FFMPEG截图文件 {} - {} : {}", camera.getNet(), camera.getCode(), path);
                    return AjaxResult.error("截图文件不存在");
                }
                byte[] result = FileUtils.readFileToByteArray(imgFile);
                imgFile.delete();
                */
                byte[] result = snap(netDevice, camera);
                return AjaxResult.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(result));
            }else if (ControlVo.CMD_PTZ.equalsIgnoreCase(controlVo.getCommand())) {
                //云台控制
                Session session = sessionMap.get(netDevice.getCode());
                if(session == null){
                    return AjaxResult.error("网关不线");
                }
                JSONObject msg = new JSONObject();
                msg.put("msgID", "01");
                msg.put("action", "StartMoveReq");

                JSONObject param = new JSONObject();
                param.put("deviceID", camera.getCode());
                param.put("x", 0);
                param.put("y", 0);
                param.put("z", 0);
                msg.put("param", param);
                String[] vs = controlVo.getValue().split(":");
                int speed = 1;
                if(vs.length > 1 && vs[1].equalsIgnoreCase("null")){
                    speed = Integer.parseInt(vs[1]);
                }
                switch (vs[0].toLowerCase()){
                    case "left":
                        param.put("x", speed);
                        break;
                    case "left_up":
                        param.put("x", speed);
                        param.put("y", speed);
                        break;
                    case "left_down":
                        param.put("x", speed);
                        param.put("y", -1*speed);
                        break;
                    case "right":
                        param.put("x", -1*speed);
                        break;
                    case "right_up":
                        param.put("x", -1*speed);
                        param.put("x", speed);
                        break;
                    case "right_down":
                        param.put("x", -1*speed);
                        param.put("y", -1*speed);
                        break;
                    case "up":
                        param.put("y", speed);
                        break;
                    case "down":
                        param.put("y", -1*speed);
                        break;
                    case "in":
                        param.put("z", speed);
                        break;
                    case "out":
                        param.put("z", -1*speed);
                        break;
                    case "stop":
                        msg.put("action", "StopMoveReq");
                        msg.remove("param");
                        break;
                    default:
                        break;
                }

                JinzhiWebSocket.sendMsg(session, msg.toJSONString());
                //设置延迟停止
                String ptzDelay = zaSysPlatform.getConfigStr("ptzDelay", "3");
                executor.schedule(()-> {
                    try {
                        msg.put("action", "StopMoveReq");
                        JinzhiWebSocket.sendMsg(session, msg.toJSONString());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                },Integer.parseInt(ptzDelay), TimeUnit.SECONDS);
                return AjaxResult.success();
            } else {
                return AjaxResult.error("不支持操作");
            }
        }catch (Exception e){
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "视频网关反控失败", e.getMessage(), JSONObject.toJSONString(controlVo));
            return AjaxResult.error("视频网关调用失败");
        }
    }

    /**
     * 生成文件名
     * @param videoRecord
     */
    private void recordName(VideoRecord videoRecord){
        String name = "";
        Date dt = DateUtils.addHours(videoRecord.getStartTime(), -8);
        name = DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS,videoRecord.getStartTime());
        //videoRecord.setStartTime(dt);
        dt = DateUtils.addHours(videoRecord.getEndTime(), -8);
        name += "_"+DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS,videoRecord.getEndTime());
        //videoRecord.setEndTime(dt);
        videoRecord.setName(name);

    }

    /**
     * 处理接收到的消息 TODO
     * @param msgObj
     * @return
     */
    @Override
    public ProcessInfo doProcessMsg(Object msgObj){
        if(!isAlive()){
            log.error("视频网关{}插件暂时停止", zaSysPlatform.getCode());
        }else{
            log.info("收到消息： {}", msgObj);
        }

        return null;
    }
}
