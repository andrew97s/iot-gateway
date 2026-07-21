package com.zhian.gateway.third.za;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.http.HttpUtils;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 智安代理网关对接，通过MQTT从智安网关接收消息，解决客户没有外网，无法接收http推送的问题
 * 1、配置 {"url":"tcp://192.168.1.198:1883","username":"test01","password":"123456","clientId":"0000000000","ability":""}
 * 2、青鸟云推送的消息
 */
@Component("zhianHandler")
@Slf4j
@DependsOn(value ="gatewayHandler")
public class ZhianHandler extends BasePlatformHandler implements MqttCallback {
    public static final String PLATFORM_NAME = "zhian";
    public static final String PROTOCOL_NAME = "za";

    private static ZaSysPlatform zaSysPlatform;

    /**
     * 客户端对象
     */
    private MqttClient client;
    private static Boolean running = false;

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        ZhianHandler.zaSysPlatform = zaSysPlatform;
        running = connect();
        return isAlive();
    }

    /**
     * 客户端连接服务端
     */
    public boolean connect() {
        try {
            //创建MQTT客户端对象
            client = new MqttClient(zaSysPlatform.getConfigStr("url"), zaSysPlatform.getConfigStr("clientId"), new MemoryPersistence());
            //连接设置
            MqttConnectOptions options = new MqttConnectOptions();
            //是否清空session，设置false表示服务器会保留客户端的连接记录（订阅主题，qos）,客户端重连之后能获取到服务器在客户端断开连接期间推送的消息
            //设置为true表示每次连接服务器都是以新的身份
            options.setCleanSession(true);
            //options.setMqttVersion(3);
            //设置连接用户名
            options.setUserName(zaSysPlatform.getConfigStr("username"));
            //设置连接密码
            options.setPassword(zaSysPlatform.getConfigStr("password").toCharArray());
            //设置超时时间，单位为秒
            options.setConnectionTimeout(100);
            //设置心跳时间 单位为秒，表示服务器每隔 1.5*20秒的时间向客户端发送心跳判断客户端是否在线
            options.setKeepAliveInterval(20);
            //设置遗嘱消息的话题，若客户端和服务器之间的连接意外断开，服务器将发布客户端的遗嘱信息
            options.setWill("willTopic", (zaSysPlatform.getConfigStr("clientId") + "与服务器断开连接").getBytes(), 0, false);
            //设置回调
            client.setCallback(this);
            client.connect(options);
            client.subscribe(zaSysPlatform.getConfigStr("clientId"));
            log.info("mqtt 连接成功");
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
            zaSysErrorService.log(ZaSysError.TYPE_MQ, "MQTT连接失败", e.getMessage(),  zaSysPlatform.getConfig());
            client = null;
        }
        return false;
    }

    @Override
    public boolean stop() {
        log.info("将忽略青鸟云平台推送过来的数据");

        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            client = null;
        }
        log.info("{} 已关闭!", zaSysPlatform.getName());
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     *
     * @return
     */
    public boolean isAlive() {
//        ZaObject object = ZaObject.get(zaSysPlatform.getConfigStr("clientId"));
//        R ret =  sendRequest("/api/proxy/heart", object.toJSON());

        return client != null && client.isConnected();
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
     * @param controlVo
     * @return
     */
    @Override
    public R doControl(ControlVo controlVo) {
        log.info("control : {}", controlVo);
        return sendRequest("/api/proxy/control", JSONObject.toJSONString(controlVo));
    }

    /**
     * 发起请求
     * @param uri
     * @param body
     * @return
     */
    private R sendRequest(String uri, String body){
        uri = "http://" + zaSysPlatform.getIp() + ":" + zaSysPlatform.getPort() + uri;
        Map<String, String> headMap = new HashMap<>();
        headMap.put("Content-Type", "application/json");
        String resStr = HttpUtils.postJSON(uri, body, headMap);
        log.info("za server post{}: {}\r\nresponse: {}", uri, body, resStr);
        return JSONObject.parseObject(resStr, R.class);
    }

    /**
     * 处理接收到的消息
     *
     * @param msgObj
     * @return
     */
    @Override
    public void processMsg(Object msgObj) {
        if (!running) {
            log.error("智安网关插件暂时停止");
            return;
        }

        MqMessage mqMessage = JSONObject.parseObject(msgObj.toString(), MqMessage.class);
        if(mqMessage == null || StringUtils.isEmpty(mqMessage.getProtocol())){
            log.error("不支持的消息体： {}", msgObj);
            return;
        }
        ThirdHandler handler = ThirdApplicationRunner.getHandler(mqMessage.getProtocol());
        if(handler == null){
            log.error("未对接的平台信息： {}", mqMessage.getProtocol());
        }else{
            handler.processMsg(mqMessage.getMsgData());
        }
    }

    /**
     * 与服务器断开的回调
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT连接断开: {}", cause);
    }

    /**
     * 消息到达的回调
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msg = new String(message.getPayload());
        log.info("收到{} 消息： {}", zaSysPlatform.getName(), msg);
        try {
            processMsg(msg);
        }catch (Exception e){
            e.printStackTrace();
            log.error("消息处理失败：{}", e);
        }
    }

    /**
     * 消息发布成功的回调
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        IMqttAsyncClient client = token.getClient();
        log.debug(client.getClientId()+"发布消息成功！");
    }

}
