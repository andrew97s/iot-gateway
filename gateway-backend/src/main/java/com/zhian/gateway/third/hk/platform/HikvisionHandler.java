package com.zhian.gateway.third.hk.platform;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.DeviceSyncInfo;
import com.zhian.gateway.third.common.bo.DeviceUpdReq;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.common.constants.MsgConstants;
import com.zhian.gateway.third.common.util.DeviceUtil;
import com.zhian.gateway.third.hk.platform.consts.PlatformConstants;
import com.zhian.gateway.third.hk.platform.vo.*;
import com.zhian.gateway.third.hk.platform.vo.*;
import com.zhian.gateway.third.video.jinzhi.JinZhiHandler;
import com.zhian.gateway.third.video.vo.VideoRecord;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zhian.gateway.third.hk.platform.consts.PlatformConstants.FACE_EVENTS;
import static com.zhian.gateway.third.hk.platform.consts.PlatformConstants.OFFLINE_EVENT;
import static java.lang.Thread.sleep;

/**
 * 海康威视平台对接
 * 1、摄像机信息
 * 2、摄像机拉流
 * 3、视频回放
 */
@Component("hikvisionHandler")
@Slf4j
@DependsOn(value = "gatewayHandler")
public class HikvisionHandler extends BasePlatformHandler<String> {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "hikvision";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "hik";
    /**
     * The constant GATE_WAY_NET_CODE.
     */
    public static final String GATE_WAY_NET_CODE = "hikvision";
    /**
     * The constant IOS8601_DATE.
     */
    public static final String IOS8601_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    /**
     * The constant PAGE_SIZE.
     */
    public static final Integer PAGE_SIZE = 100;
    /**
     * The constant zaSysPlatform.
     */
    private static ZaSysPlatform zaSysPlatform;
    /**
     * The Artemis config.
     */
    private ArtemisConfig artemisConfig = null;
    //延迟任务，主要是云台控制使用
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


    @Override
    public boolean start(ZaSysPlatform platform) {
        HikvisionHandler.zaSysPlatform = platform;
        artemisConfig = new ArtemisConfig();
        artemisConfig.setHost(platform.getIp()); //平台（nginx）IP 和端口
        artemisConfig.setAppKey(platform.getConfigStr("key")); //合作方 key
        artemisConfig.setAppSecret(platform.getConfigStr("secret"));//合作方 Secret

        log.info("开始对接海康平台 {}: {}", platform.getIp(), platform.getConfig());
        //注册网关
        addGateway();
        //注册平台
        syncCamera();
        // 尝试订阅海康平台事件
        trySubscribe(platform);
        running = true;
        return true;
    }

    @Override
    public boolean stop() {
        running = false;
        artemisConfig = null;
        return true;
    }

    @Override
    public DeviceSyncInfo syncDeviceStatus() {
        syncCameraOnline();
        return DeviceSyncInfo.success(1);
    }

    @Override
    public boolean isAlive() {
        return running;
    }

    @Override
    public ProcessInfo doProcessMsg(String hkEvent) {
        // TODO 海康事件对象过于复杂-此处直接解析成JSON(需后期封装对应VO处理)
        JSONObject event = JSON.parseObject(hkEvent);

        String imageUrl = findValue(event, "imageUrl");
        String deviceCode = findValue(event, "srcIndex");
        String eventType = findValue(event, "eventType");
        String status = findValue(event, "status");
        DateTime alarmTime = DateUtil.parse(findValue(event, "happenTime"));
        ZaSysDevice device = null;

        if (StrUtil.isNotBlank(deviceCode) && (Constants.YES.equals(status) || Constants.NO.equals(status))) {
            device = deviceService.selectZaSysDeviceByCode(deviceCode, "hikvision");
            if (device == null) {
                log.warn("海康事件处理失败,设备(code:{})不存在!", deviceCode);
                return null;
            }
        } else {
            log.warn("海康事件处理失败,消息未提取到有效设备编码字段!");
        }
        List<MqMessage> msgList = new ArrayList<>();
        String msgType = MsgConstants.MSG_TYPE_BUSINESS;

        if (StrUtil.isNotBlank(eventType) && device != null) {
            // 人脸业务事件
            if (FACE_EVENTS.contains(eventType)) {
                imageUrl = findValue(event, "bkgUrl");
                // 保存消息
                msgList.add(
                        MqMessage.createBusiness(device, eventType, getProtocol(), hkEvent, imageUrl)
                );
            }
            // 告警事件
            else {
                // 尝试手动抓图
                if (StrUtil.isBlank(imageUrl)) {
                    Map<String, Object> params = new HashMap<>();//post 请求的查询参数
                    params.put("cameraIndexCode", device.getCode());
                    try {
                        imageUrl = request(
                                "/api/video/v1/manualCapture", params, HikUrlResult.class
                        ).getData().getPicUrl();
                    } catch (Exception e) {
                        log.error("海康手动抓图失败,msg:{}", e.getMessage());
                    }
                }
                // 处理离线告警
                if (OFFLINE_EVENT.equals(eventType) && Constants.YES.equals(device.getOnline())) {
                    device.setOnline(Constants.NO);
                    deviceService.updateZaSysDevice(device);
                }

                msgList.add(genAlarm(device, eventType, imageUrl, hkEvent));
                msgType = MsgConstants.MSG_TYPE_ALARM;
            }

            return ProcessInfo.newInstance(device, hkEvent, msgType, msgList);
        }

        return null;
    }

    public static String findValue(Object json, String key) {
        Stack<Object> stack = new Stack<>();
        stack.push(json);

        while (!stack.isEmpty()) {
            Object current = stack.pop();

            if (current instanceof JSONObject) {
                JSONObject obj = (JSONObject) current;
                for (String k : obj.keySet()) {
                    if (k.equals(key)) {
                        return obj.get(k).toString(); // 找到目标键
                    }
                    stack.push(obj.get(k)); // 加入子节点
                }
            } else if (current instanceof JSONArray) {
                JSONArray array = (JSONArray) current;
                for (int i = 0; i < array.size(); i++) {
                    stack.push(array.get(i)); // 将数组元素加入栈
                }
            }
        }

        return ""; // 未找到目标键
    }


    /**
     * 从海康平台同步摄像机状态信息
     */
    private void syncCameraOnline() {
        log.info("开始同步海康平台的摄像机状态");
        Set<String> cameraIds = new HashSet<>();
        Map<String, Object> params = new HashMap<>();//post 请求的查询参数
        params.put("pageSize", PAGE_SIZE);
        new Thread(() -> {
            try {
                int page = 1;
                while (true) {
                    params.put("pageNo", page);
                    HikOnlineResult result = request("/api/nms/v1/online/camera/get", params, HikOnlineResult.class);
                    if (!result.isSuccess()) {
                        log.error("获取摄像机信息失败: {} - {}", result.getCode(), result.getMsg());
                        break;
                    }

                    if (result.getData().getList() == null || result.getData().getList().length == 0) {
                        break;
                    }

                    for (HikOnline status : result.getData().getList()) {
                        ZaSysDevice camera = deviceService.selectZaSysDeviceByCode(status.getDeviceIndexCode(), GATE_WAY_NET_CODE);
                        if (camera != null && !camera.getOnline().equalsIgnoreCase(status.getOnline())) {
                            boolean offline = StrUtil.equals(status.getOnline(), "0");
                            DeviceUtil.pushDevice(
                                    offline ?
                                            DeviceUpdReq.newOfflineReq(camera, null) :
                                            DeviceUpdReq.newOnlineReq(camera, null)
                            );
                        }
                        cameraIds.add(status.getDeviceIndexCode());
                    }
                    if (cameraIds.size() < result.getData().getTotal()) {
                        page++;
                        sleep(1000);
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public AjaxResult doControl(ControlVo controlVo) {
        ZaSysDevice camera = controlVo.getDevice();
        if (camera == null) {
            return AjaxResult.error("设备信息不存在");
        }
        switch (controlVo.getCommand()) {
            // 拉流
            case ControlVo.CMD_STREAM: {
                return fetchStream(camera);
            }
            // 回放
            case ControlVo.CMD_PLAY_BACK: {
                return fetchPlayback(camera, controlVo);
            }
            // 查询回放记录
            case ControlVo.CMD_RECORDS: {
                return fetchPlayRecords(controlVo);
            }
            // 截图
            case ControlVo.CMD_SNAP: {
                return fetchSnapshot(camera);
            }
            // 云台控制
            case ControlVo.CMD_PTZ: {
                return ptz(camera, controlVo.getValue());
            }
            // 查询
            case ControlVo.CMD_QRY: {
                return qryFaceRecords(controlVo.getValue());
            }
            default:
                return AjaxResult.error("不支持操作");
        }
    }

    /**
     * 尝试订阅海康告警事件
     * <p>
     * 告警事件见
     * <a
     * href="https://open.hikvision.com/docs/docId?productId=5c67f1e2f05948198c909700&version=%2F29c78ef52ca
     * 842c7933bd2b8e051e9d0&tagPath=%E9%99%84%E5%BD%95-%E9%99%84%E5%BD%95D%20%E4%BA%8B%E4%BB%B6%E5%88%97%E8%A1%A8">
     * 链接</a>
     *
     * @param platform platform
     */
    private void trySubscribe(ZaSysPlatform platform) {
        if (Constants.NO.equals(platform.getConfigStr("needSub", Constants.NO))) {
            return;
        }
        // 事件订阅
        HikSubReq req = new HikSubReq();
        String subEvents = platform.getConfigStr("subEvents", PlatformConstants.DEFAULT_SUB_EVENTS);
        req.setEventTypes(
                StrUtil.split(subEvents, ",", true, true)
                        .stream().map(Integer::parseInt).collect(Collectors.toList())
        );
        req.setEventDest(
                platform.getConfigStr("event_webhook_url", "http://localhost:9200/api/hk_platform/event_webhook")
        );
        try {
            HikResult result = request(
                    "/api/eventService/v1/eventSubscriptionByEventTypes", req.getRequestAsMap(), HikResult.class
            );
            log.info("海康平台订阅事件请求响应:{}", JSON.toJSONString(result));
        } catch (Exception e) {
            log.error("海康平台订阅事件请求失败,msg:{}", e.getMessage());
        }
    }

    private AjaxResult qryFaceRecords(String params) {
        HashMap queryMap = JSONObject.parseObject(params, HashMap.class);
        try {

            return AjaxResult.success(
                    request("/api/frs/v1/application/captureSearch", queryMap, HikFaceResp.class).getData()
            );
        } catch (Exception e) {
            log.error("查询海康人脸数据失败，msg:{}", e.getMessage());
            return AjaxResult.error("操作失败,调用海康人脸查询接口失败!");
        }
    }

    /**
     * 拉流
     *
     * @param device the device
     * @return the ajax result
     */
    private AjaxResult fetchStream(ZaSysDevice device) {
        Map<String, Object> params = new HashMap<>();
        params.put("cameraIndexCode", device.getCode());
        // 码流类型0:主码流 1:子码流 2:第三码流 ， 默认为主码流
        params.put("streamType", zaSysPlatform.getConfigStr("streamType", "1"));
        // 协议-使用ws
        params.put("protocol", HikvisionHandler.zaSysPlatform.getConfigStr("protocol", "ws"));
        try {
            HikUrlResult result = request("/api/video/v2/cameras/previewURLs", params, HikUrlResult.class);
            if (result.isSuccess()) {
                String stream = result.getData().getUrl();
                return AjaxResult.success(stream, "hk");
            } else {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台拉流失败", result.getCode(), result.getMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台拉流异常", e.getMessage(), null);
        }
        return AjaxResult.error("获取视频流失败");
    }

    /**
     * 查询回放
     *
     * @param device    the device
     * @param controlVo the control vo
     * @return the ajax result
     */
    private AjaxResult fetchPlayback(ZaSysDevice device, ControlVo controlVo) {
        Map<String, Object> params = new HashMap<>();//post 请求的查询参数
        params.put("cameraIndexCode", device.getCode());
        params.put("protocol", HikvisionHandler.zaSysPlatform.getConfigStr("protocol", "ws"));
        String[] vs = controlVo.getValue().split("_");
        //2017-06-15T00:00:00.000+08:00
        params.put("beginTime", vs[0]);
        params.put("endTime", vs[1]);
        try {
            HikUrlResult result = request("/api/video/v2/cameras/playbackURLs", params, HikUrlResult.class);
            if (result.isSuccess()) {
                AjaxResult ajaxResult = AjaxResult.success(result.getData().getUrl());
                HashMap<String, Object> data = new HashMap<>();
                data.put("playUrl", result.getData().getUrl());
                data.put("playType", "hk");
                ajaxResult.put("data", data);

                return ajaxResult;
            } else {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台拉回放流失败", result.getCode(), result.getMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台拉回放流异常", e.getMessage(), null);
        }
        return AjaxResult.error("获取视频回放流失败");
    }

    /**
     * 查询回放记录
     *
     * @param controlVo the control vo
     * @return the ajax result
     */
    private AjaxResult fetchPlayRecords(ControlVo controlVo) {
        List<VideoRecord> list = new ArrayList<>();
        Date today = new Date();
        Date queryDay = DateUtils.dateTime(DateUtils.YYYY_MM_DD, controlVo.getValue());
        int hours = (controlVo.getValue().equalsIgnoreCase(DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD))) ? today.getHours() : 24;
        for (int i = 0; i < hours; i++) {
            VideoRecord videoRecord = new VideoRecord();
            videoRecord.setStartTime(i > 0 ? DateUtils.addHours(queryDay, i) : queryDay);
            videoRecord.setEndTime(DateUtils.addHours(queryDay, i + 1));
            videoRecord.setName(DateUtils.parseDateToStr(IOS8601_DATE, videoRecord.getStartTime()) + "_" + DateUtils.parseDateToStr(IOS8601_DATE, videoRecord.getEndTime()));
            list.add(videoRecord);
        }
        return AjaxResult.success(list);
    }

    /**
     * 截图
     *
     * @param device the device
     * @return the ajax result
     */
    private AjaxResult fetchSnapshot(ZaSysDevice device) {
        //截图
        Map<String, Object> params = new HashMap<>();//post 请求的查询参数
        params.put("cameraIndexCode", device.getCode());
        try {
            HikUrlResult result = request("/api/video/v1/manualCapture", params, HikUrlResult.class);
            if (result.isSuccess()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                HttpUtil.download(result.getData().getPicUrl(), os, true);
                return AjaxResult.success("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(os.toByteArray()));
            } else {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台摄像机 " + device.getName() + " 截图失败", result.getCode(), result.getMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台摄像机 " + device.getName() + "截图失败异常", e.getMessage(), null);
        }
        return AjaxResult.error();
    }

    /**
     * 云台控制LEFT 左转
     * //            RIGHT右转
     * //            UP 上转
     * //            DOWN 下转
     * //            ZOOM_IN 焦距变大
     * //            ZOOM_OUT 焦距变小
     * //            LEFT_UP 左上
     * //            LEFT_DOWN 左下
     * //            RIGHT_UP 右上
     * //            RIGHT_DOWN 右下
     *
     * @param device   the device
     * @param cmdValue 指令
     * @return the ajax result
     */
    private AjaxResult ptz(ZaSysDevice device, String cmdValue) {
        String[] vs = cmdValue.split(":");
        int speed = (vs.length > 1) ? Integer.parseInt(vs[1]) * 10 : 50;

        Map<String, Object> params = new HashMap<>();//post 请求的查询参数
        params.put("cameraIndexCode", device.getCode());
        params.put("action", 0);
        params.put("command", vs[0]);
        params.put("speed", speed);
        try {
            HikUrlResult result = request("/api/video/v1/ptzs/controlling", params, HikUrlResult.class);
            if (result.isSuccess()) {
                //设置延迟停止
                String ptzDelay = zaSysPlatform.getConfigStr("ptzDelay", "3");
                executor.schedule(() -> {
                    try {
                        params.put("action", 1);
                        request("/api/video/v1/ptzs/controlling", params, HikUrlResult.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, Integer.parseInt(ptzDelay), TimeUnit.SECONDS);
                return AjaxResult.success();
            } else {
                zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台摄像机 " + device.getName() + " 云台控制失败", result.getCode(), result.getMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_API_ERROR, "海康平台摄像机 " + device.getName() + " 云台控制失败", e.getMessage(), null);
        }
        return AjaxResult.error();
    }

    /**
     * 注册平台为网关
     */
    private void addGateway() {
        ZaSysDevice dc = deviceService.selectZaSysDeviceByCode(GATE_WAY_NET_CODE, GATE_WAY_NET_CODE);
        if (dc != null) {
            //如果设备已存在，更新IP
            if (!dc.getIp().equalsIgnoreCase(zaSysPlatform.getIp())) {
                dc.setIp(zaSysPlatform.getIp());
                deviceService.updateZaSysDevice(dc);
            }
            // 同步网关设备状态
            DeviceUtil.pushDevice(DeviceUpdReq.newOnlineReq(dc, null));
        } else {
            //保存新设备信息
            dc = new ZaSysDevice();
            dc.setCode(GATE_WAY_NET_CODE);
            dc.setNet(GATE_WAY_NET_CODE);
            dc.setType(DeviceType.VAG);
            dc.setModel("海康安防平台");
            dc.setOnline("1");
            dc.setWireless("0");
            dc.setName("海康安防平台 " + zaSysPlatform.getIp());
            dc.setPfCode(JinZhiHandler.PLATFORM_NAME);
            dc.setIp(zaSysPlatform.getIp());
            dc.setOnline("1");
            deviceService.insertZaSysDevice(dc);
            // 同步网关设备状态
            DeviceUtil.pushDevice(DeviceUpdReq.newAddReq(dc, null));
        }
    }

    /**
     * 从海康平台同步摄像机信息
     */
    private void syncCamera() {
        log.info("开始同步海康平台的摄像机信息");
        Set<String> cameraIds = new HashSet<>();
        Map<String, Object> params = new HashMap<>();//post 请求的查询参数
        params.put("pageSize", PAGE_SIZE);
        new Thread(() -> {
            try {
                int page = 1;
                while (true) {
                    params.put("pageNo", page + "");
                    HikCameraResult result = request("/api/resource/v1/cameras", params, HikCameraResult.class);
                    if (!result.isSuccess()) {
                        log.error("获取摄像机信息失败: {} - {}", result.getCode(), result.getMsg());
                        break;
                    }

                    if (result.getData().getList() == null || result.getData().getList().length == 0) {
                        break;
                    }

                    for (HikCamera camera : result.getData().getList()) {
                        ZaSysDevice dc = addCamera(camera);
                        cameraIds.add(dc.getCode());
                    }
                    if (cameraIds.size() < result.getData().getTotal()) {
                        page++;
                        sleep(1000);
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        //删除不存在的摄像机
        ZaSysDevice dc = new ZaSysDevice();
        dc.setNet(GATE_WAY_NET_CODE);
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        for (ZaSysDevice camera : list) {
            if (!cameraIds.contains(camera.getCode())) {
                DeviceUtil.pushDevice(DeviceUpdReq.newDelReq(camera, null));
            }
        }

        //同步状态
        syncCameraOnline();
    }

    /**
     * 注册摄像机设备
     *
     * @param camera the camera
     * @return za sys device
     */
    private ZaSysDevice addCamera(HikCamera camera) {
        //先根据deviceID和网关，查询设备
        ZaSysDevice dc = deviceService.selectZaSysDeviceByCode(camera.getCameraIndexCode(), GATE_WAY_NET_CODE);
        if (dc != null) {
            //如果设备已存在，更新remark扩展信息
            if (StringUtils.isEmpty(dc.getRemark())) {
                dc.setRemark(JSONObject.toJSONString(camera));
                deviceService.updateZaSysDevice(dc);
            }
            return dc;
        } else {
            //保存新设备信息
            dc = new ZaSysDevice();
            dc.setCode(camera.getCameraIndexCode());
            dc.setNet(GATE_WAY_NET_CODE);
            dc.setType(DeviceType.CAMERA);
            dc.setOnline("1");
            dc.setWireless("0");
            dc.setName(camera.getCameraName());
            dc.setPfCode(PLATFORM_NAME);
            dc.setIp(null);
            dc.setOnline((camera.getStatus() != null && !camera.getStatus().equalsIgnoreCase("1")) ? "0" : "1");
            dc.setRemark(JSONObject.toJSONString(camera));
            deviceService.insertZaSysDevice(dc);
        }

        // 推送到平台
        DeviceUtil.pushDevice(DeviceUpdReq.newAddReq(dc, JSONObject.toJSONString(camera)));
        return dc;
    }

    /**
     * 发起请求，并解析结果
     *
     * @param <T>       the type parameter
     * @param api       the api
     * @param params    the params
     * @param typeClass the type class
     * @return t
     * @throws Exception the exception
     */
    private <T> T request(String api, Map<String, Object> params, Class<T> typeClass) throws Exception {
        Map<String, String> path = new HashMap<String, String>();
        path.put("https://", "/artemis" + api);

        String result = ArtemisHttpUtil.doPostStringArtemis(artemisConfig, path, JSONObject.toJSONString(params),
                null, null, "application/json", null);// post请求application/json类型参数
        log.info("hikvision post {}, response: \r\n {}", api, result);
        return JSONObject.parseObject(result, typeClass);
    }

}
