package com.zhian.gateway.third.cascade;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.spring.SpringUtils;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import com.zhian.gateway.third.vo.ControlVo;
import com.zhian.gateway.third.vo.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 级联对接服务
 * 1、通过WebSocket连接上级网关平台
 * 2、当本平台接收到消息后，除了向RabbitMQ推送之外，还会向上级平台进行推送
 * 3、接收到上级平台下发的控制指令之后，转发指令，并将结果推送到上级平台
 */
@Component
@Slf4j
public class CascadeHandler implements ThirdHandler {
    public static final String PLATFORM_NAME = "cascade";
    private static ZaSysPlatform zaSysPlatform;
    private static Session session;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Override
    public boolean start(ZaSysPlatform zaSysPlatform) {
        CascadeHandler.zaSysPlatform = zaSysPlatform;
        if(isAlive()){
            return true;
        }

        JSONObject config = null;
        if(StringUtils.isEmpty(zaSysPlatform.getConfig())){
            config = new JSONObject();
            config.put("id",SnowflakeIdWorker.getInstance().nextId());
            zaSysPlatform.setConfig(config.toJSONString());
            SpringUtils.getBean(IZaSysPlatformService.class).updateZaSysPlatform(zaSysPlatform);
        }else{
            config = JSONObject.parseObject(zaSysPlatform.getConfig());
        }
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            //设置消息大小最大为10M
            container.setDefaultMaxBinaryMessageBufferSize(10 * 1024 * 1024);
            container.setDefaultMaxTextMessageBufferSize(10 * 1024 * 1024);
            // 客户端，开启服务端websocket。
            String uri = "ws://"+zaSysPlatform.getIp()+":"+zaSysPlatform.getPort()+"/cascade/"+config.getString("id");
            log.info("连接级联websocket {} ", uri);
            session = container.connectToServer(CascadeClientEndpoint.class, URI.create(uri));
            keepAlive();
        } catch (Exception ex) {
            log.error("连接websocket {} 失败", ex.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 定时发送心跳包
     */
    private void keepAlive(){
        executorService.scheduleAtFixedRate(() -> {
            if(isAlive()){
                processMsg("{\"heart\":\"auto\",\"ws\":\"cascade\",\"systime\":"+System.currentTimeMillis()+"}");
            }else if(zaSysPlatform.getStatus().equalsIgnoreCase("1")){
                log.error("级联对接已经停止，将重新连接: {}", zaSysPlatform.getIp());
                start(zaSysPlatform);
            }else{
                log.error("级联对接未连接: {}", zaSysPlatform.getIp());

            }
        },1L, 1L, TimeUnit.MINUTES);
    }

    @Override
    public boolean stop() {
        if(session == null || !session.isOpen()){
            session = null;
            return true;
        }

        try {
            session.close();
            session = null;
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean isAlive(){
        return session != null && session.isOpen();
    }

    @Override
    public String getPlatform() {
        return PLATFORM_NAME;
    }

    @Override
    public String getProtocol() {
        return PLATFORM_NAME;
    }

    @Override
    public R control(ControlVo controlVo) {
        if(!isAlive()){
            log.error("级联插件暂时停止");
            return R.error("级联插件暂时停止");
        }
        ZaSysDevice zaSysDevice = controlVo.getDevice();
        if(session == null){
            return R.error("设备不存在");
        }
        ZaSysPlatform devicePlatform = SpringUtils.getBean(IZaSysPlatformService.class)
                .selectZaSysPlatformByCode(zaSysDevice.getPfCode());

        return ThirdApplicationRunner.getHandler(devicePlatform.getCode()).control(controlVo);
    }

    /**
     * 级联端接收到的是上级下发的指令，当作反向控制处理
     * @param msgObj
     */
    @Override
    public void processMsg(Object msgObj) {
        if(!isAlive()){
            log.error("级联插件暂时停止");
            return;
        }
        ControlVo controlVo = null;
        if(msgObj instanceof ControlVo){
            controlVo = (ControlVo) msgObj;
        }else{
            controlVo = JSONObject.parseObject(msgObj.toString(), ControlVo.class);
        }
        if(controlVo.getDeviceId() == null){
            log.info("非反控信息，将忽略");
            return;
        }

        //通过其它插件进行反控
        R result = control(controlVo);
        MqMessage mqMessage = new MqMessage();
        mqMessage.setProtocol(PLATFORM_NAME);
        mqMessage.setUuid(controlVo.getEventId());
        mqMessage.setEvent(MqMessage.EVENT_CASCADE);
        mqMessage.setMsgData(JSONObject.toJSONString(result));
        pushMsg(mqMessage);
    }

    public void pushMsg(Object msgObj) {
        if(!isAlive()){
            //log.error("级联插件暂时停止");
            return;
        }
        log.info("push msg to cascade: {}", msgObj);
        try {
            String msgStr = null;
            if(msgObj instanceof String){
                msgStr = msgObj.toString();
            }else{
                msgStr = JSONObject.toJSONString(msgObj);
            }
            session.getBasicRemote().sendText(msgStr);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
