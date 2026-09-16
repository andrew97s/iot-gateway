package com.zhian.gateway.third.video.roc.ws;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.third.video.roc.RocHandler;
import com.zhian.gateway.third.video.roc.common.RocConstants;
import com.zhian.gateway.third.video.roc.common.RocContext;
import com.zhian.gateway.third.video.roc.common.RocMsg;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.MessageHandler;
import java.util.Collections;

/**
 * Roc消息业务处理
 */
@Slf4j
public class RocMessageHandler implements MessageHandler.Whole<String> {

    @Override
    public void onMessage(String message) {
        log.info("接收到ROC WS 消息： {}", message);
        RocMsg msg = JSON.parseObject(message, RocMsg.class);

        //认证信息
        RocContext msgContext = RocContext.getInstance();
        if(msg.getAuth() != null) {
            // 重新赋值消息认证属性
            String qop = msg.getAuth().getQop();
            String realm = msg.getAuth().getRealm();
            String nonce = msg.getAuth().getNonce();
            String nextNonce = msg.getAuth().getNextnonce();

            msgContext.setQop(qop);
            msgContext.setRealm(realm);
            msgContext.setNonce(StrUtil.isNotBlank(nextNonce) ? nextNonce : nonce);
            msgContext.setSequence(msgContext.getSequence() != null ? msgContext.getSequence() + 1 : 1L);
        }

        if (msg.getMethod().equalsIgnoreCase("POST") && msg.getUri().equalsIgnoreCase(RocConstants.URI_SESSION)) {
            loginCallback(msg);
        }else if (msg.getMethod().equalsIgnoreCase("GET") && msg.getUri().equalsIgnoreCase(RocConstants.URI_FACE_RULE)) {
            faceRuleCallback(msg);
        }else if (msg.getMethod().equalsIgnoreCase("PUT") && msg.getUri().equalsIgnoreCase(RocConstants.URI_SET_PUSH)) {
            httpPushCallback(msg);
        }else if (msg.getMethod().equalsIgnoreCase("POST") && msg.getUri().equalsIgnoreCase(RocConstants.URI_FACE_GROUP_ADD)) {
            faceGroupAddCallback(msg);
        }else if (msg.getMethod().equalsIgnoreCase("POST") && msg.getUri().equalsIgnoreCase(RocConstants.URI_FACE_SEARCH)) {
            faceSearchCallback(msg);
        }else if (msg.getMethod().equalsIgnoreCase("GET") && msg.getUri().equalsIgnoreCase(RocConstants.URI_DEVICE_INFO)) {
            deviceInfoCallback(msg);
        }else{
            log.debug("忽略ROC WS消息: {} - {}", msg.getMethod(), msg.getUri());
        }
    }

    /**
     * 登录成功，开启人脸识别
     * @param rocMsg
     */
    public void loginCallback(RocMsg rocMsg){
        RocHandler handler = SpringUtils.getBean(RocHandler.class);
        //设备信息
        handler.sendToServer(RocMsg.create(RocConstants.METHOD_GET, RocConstants.URI_DEVICE_INFO));

        //测试
        handler.sendToServer(RocMsg.create(RocConstants.METHOD_GET, RocConstants.URI_SET_PUSH));

        //开启人脸识别
        handler.sendToServer(RocMsg.create(RocConstants.METHOD_GET, RocConstants.URI_FACE_RULE));


        //配置事件推送
        String ip = NetUtil.getLocalhost().getHostAddress();
        JSONObject push = new JSONObject();
        push.put("Auth", 0);
        push.put("Enable", true);
        push.put("HeartInterval", 60);
        push.put("Username", RocHandler.zaSysPlatform.getConfigStr("username", "admin"));
        push.put("Password", RocHandler.zaSysPlatform.getConfigStr("password", "123456"));
        push.put("ServerAddr", "http://" + ip + ":9200/");
        RocMsg req = RocMsg.create(RocConstants.METHOD_PUT, RocConstants.URI_SET_PUSH);
        req.setBody(Collections.singletonMap("HttpPushCfg", push));
        handler.sendToServer(req);


        //查询全部人脸
        RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_SEARCH);
        JSONObject body = new JSONObject();
        body.put("maxResults", 1000);
        body.put("searchType", 0);
        body.put("groupName", RocConstants.GROUP_NAME);
        msg.setBody(Collections.singletonMap("FRSearchDescription", body));
        handler.sendToServer(msg);
    }

    /**
     * 设置了人脸识别区域，就开启人脸识别功能
     * @param msg
     */
    public void faceRuleCallback(RocMsg msg){
        RocContext msgContext = RocContext.getInstance();
        msgContext.setFaceRule(
                msg.getBody() instanceof String ?
                        msg.getBody().toString() : JSON.toJSONString(msg.getBody())
        );

        if (StrUtil.isBlank(msgContext.getFaceRule())) {
            log.error("ROC摄像机没有配置人脸区域");
            return;
        }
        JSONObject ret = JSONObject.parseObject(msgContext.getFaceRule());
        ret  = ret.getJSONObject("FaceRule");
        if(!ret.getBoolean("enabled")){
            log.info("ROC摄像机未启动人脸识别，将启动");
            String faceRule = msgContext.getFaceRule();

            // 使用正则表达式替换 enabled 属性的值
            faceRule = faceRule.replaceAll("\"enabled\":\\s*false", "\"enabled\":true");

            JSONObject ruleBody = JSON.parseObject(faceRule);
            RocMsg req = RocMsg.create(RocConstants.METHOD_PUT, RocConstants.URI_FACE_RULE, ruleBody);

            SpringUtils.getBean(RocHandler.class).sendToServer(req);
        }else{
            log.info("ROC摄像机已经启动人脸识别");
            msgContext.setFaceEnable(true);;
        }
    }

    public void httpPushCallback(RocMsg rocMsg){
        //自动创建分组
        RocMsg msg = RocMsg.create(RocConstants.METHOD_POST, RocConstants.URI_FACE_GROUP_ADD);
        JSONObject body = new JSONObject();
        body.put("enable", true);
        body.put("groupName", RocConstants.GROUP_NAME);
        body.put("groupType", "white");
        body.put("similarity", 80);
        msg.setBody(Collections.singletonMap("FRAddDescription", body));
        SpringUtils.getBean(RocHandler.class).sendToServer(msg);
    }

    public void faceGroupAddCallback(RocMsg msg){
        RocContext msgContext = RocContext.getInstance();
        JSONObject ret = msg.getBody() instanceof JSONObject ? (JSONObject) msg.getBody() : JSONObject.parseObject(msg.getBody().toString());
        ret = ret.getJSONObject("FRAddResult");
        if(ret.getInteger("statusCode") == 0 || ret.getInteger("errorCode") == -900){
            msgContext.setFaceGroup(true);
        }
    }

    public void deviceInfoCallback(RocMsg msg){
        RocContext msgContext = RocContext.getInstance();
        JSONObject ret = msg.getBody() instanceof JSONObject ? (JSONObject) msg.getBody() : JSONObject.parseObject(msg.getBody().toString());
        ret = ret.getJSONObject("DeviceInfo");
        msgContext.setDeviceName(ret.getString("deviceName"));
        msgContext.setSerialNum(ret.getString("serialNumber"));
        SpringUtils.getBean(RocHandler.class).pushDevice();
    }

    public void faceSearchCallback(RocMsg rocMsg){
        JSONObject ret = rocMsg.getBody() instanceof JSONObject ? (JSONObject) rocMsg.getBody() : JSONObject.parseObject(rocMsg.getBody().toString());
        ret = ret.getJSONObject("FRSearchResult").getJSONObject("matchList");
        JSONArray list = ret.getJSONArray("searchMatchItem");
        if(list == null || list.isEmpty()){
            return;
        }
        for(int i=0;i<list.size();i++){
            RocContext.getInstance().getFaceSet().add(list.getJSONObject(i).getString("name"));
        }
    }
}
