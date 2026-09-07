package com.zhian.gateway.third.video.jinzhi;

import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.third.PluginHealthResult;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.common.bo.ProcessInfo;
import com.zhian.gateway.third.common.constants.DeviceType;
import com.zhian.gateway.third.video.jinzhi.bo.JzMessage;
import com.zhian.gateway.third.video.jinzhi.util.JzControlUtil;
import com.zhian.gateway.third.video.jinzhi.util.JzMsgUtil;
import com.zhian.gateway.third.vo.ControlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * 金智视频网关对接
 */
@Component
@Slf4j
public class JinZhiHandler extends BasePlatformHandler<JzMessage> {
    /**
     * The constant PLATFORM_NAME.
     */
    public static final String PLATFORM_NAME = "jinzhi";
    /**
     * The constant PROTOCOL_NAME.
     */
    public static final String PROTOCOL_NAME = "jz";

    /**
     * 所有会话对象
     */
    public static Map<String, Session> sessionMap = new HashMap<>();

    @Override
    public boolean stop() {
        closeAll();
        return super.stop();
    }

    @Override
    public boolean start(ZaSysPlatform platform) {
        super.start(platform);
        JzMsgUtil.init(platform);
        JzControlUtil.init(platform);
        return true;
    }

    /**
     * 确认第三方对接服务是否正常
     *
     * @return true or false
     */
    @Override
    public boolean isAlive() {
        // TODO 设备状态同步
        ZaSysDevice dc = new ZaSysDevice();
        dc.setType(DeviceType.VAG);
        dc.setModel(JinzhiConst.MODEL_NAME);
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(dc);
        for (ZaSysDevice gateway : list) {
            //原网关不在线，现在上线了
            if (sessionMap.containsKey(gateway.getCode()) && "0".equalsIgnoreCase(gateway.getOnline())) {
                gateway.setOnline("1");
                deviceService.updateZaSysDevice(gateway);
                // TODO
//                pushState(gateway, AlarmType.ONLINE.getCode());
            } else if (!sessionMap.containsKey(gateway.getCode()) && "1".equalsIgnoreCase(gateway.getOnline())) {
                //原网关在线，现在不在线了
                gateway.setOnline("0");
                deviceService.updateZaSysDevice(gateway);
//                pushState(gateway, AlarmType.OFFLINE.getCode());
            }
        }
        return running;
    }

    @Override
    public PluginHealthResult checkHealth() {
        if (!running) {
            return PluginHealthResult.unhealthy("插件未运行");
        }
        int open = 0;
        for (Session session : new ArrayList<>(sessionMap.values())) {
            if (session != null && session.isOpen()) {
                open++;
            }
        }
        return PluginHealthResult.healthy("WebSocket 接入端已就绪，当前连接 " + open + " 路");
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, "服务器暂时关闭");
        Set<String> ss = new HashSet<>();
        ss.addAll(sessionMap.keySet());
        for (String k : ss) {
            if (!sessionMap.containsKey(k)) {
                continue;
            }
            Session session = sessionMap.get(k);
            log.info("主动关闭视频网关的ws连接 {}", session.getId());
            try {
                session.close(closeReason);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sessionMap.clear();
    }

    /**
     * 网关下线
     *
     * @param gateway the gateway
     */
    public void removeGateway(ZaSysDevice gateway) {
        sessionMap.remove(gateway.getCode());
        log.info("网关 {}-{}-{} 已下线", gateway.getModel(), gateway.getIp(), gateway.getCode());
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
     *
     * @param controlVo controlVo
     * @return null
     */
    @Override
    public R doControl(ControlVo controlVo) {
        return JzControlUtil.control(controlVo);
    }

    /**
     * 处理接收到的消息
     *
     * @param msg msg
     * @return null
     */
    @Override
    public ProcessInfo doProcessMsg(JzMessage msg) {
        JzMsgUtil.processMessage(msg);
        return null;
    }
}
