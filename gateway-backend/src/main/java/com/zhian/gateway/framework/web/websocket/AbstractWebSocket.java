package com.zhian.gateway.framework.web.websocket;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.framework.web.websocket.config.WebSocketFilter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zhian.gateway.framework.web.websocket.consts.WebsocketConstants.DEFAULT_INTERFACE;
import static com.zhian.gateway.framework.web.websocket.consts.WebsocketConstants.IGNORE_LOCAL_IP;

/**
 * The type Abstract web socket.
 */
@Slf4j
@SuppressWarnings("CallToPrintStackTrace")
public abstract class AbstractWebSocket {

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private final static ConcurrentHashMap<Class<?>, AtomicInteger> onlineCount = new ConcurrentHashMap<>();

    /**
     * 获取在线总数
     *
     * @return int
     */
    public int getOnlineCount(){
        return onlineCount.getOrDefault(this.getClass() , new AtomicInteger(0)).intValue();
    }

    /**
     * 客户端在线计数
     *
     * @param count the count
     */
    protected synchronized void calcOnlineCount(int count) {
        Class<?> currentClass = this.getClass();
        AtomicInteger onLineCount = onlineCount.get(currentClass);
        if (onLineCount == null) {
            onLineCount = new AtomicInteger(0);
            onlineCount.put(currentClass, onLineCount);
        }
        onLineCount.addAndGet(count);
    }

    /**
     * 获取客户端IP
     *
     * @param config the config
     * @return client ip address
     */
    protected String getClientIpAddress(EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());

        String ip = httpSession.getAttribute(WebSocketFilter.IP_ATTR_NAME).toString();
        log.info(" remote ws ip : {}" ,ip );
        // 本地IP转换
        if (StrUtil.isNotBlank(ip) && (ip.equals("127.0.0.1") || ip.equals("localhost"))) {
            // 直接获取eth0的IP
            ip = getIpByInterfaceName(DEFAULT_INTERFACE);
            if (StrUtil.isNotBlank(ip) && !ip.equals(IGNORE_LOCAL_IP)) {
                return ip;
            }

            LinkedHashSet<String> ips = NetUtil.localIpv4s();
            for (String ipp : ips) {
                // 非etho网卡 ， 需要忽略部分IP
                if (!ipp.equals("127.0.0.1") && !ipp.equals("localhost") && !ipp.equals(IGNORE_LOCAL_IP)) {
                    ip = ipp;
                    break;
                }
            }

            log.info("  local ip transfer : {}" ,ip );
        }
        return ip;
    }

    /**
     * Gets ip by interface name.
     *
     * @param interfaceName the interface name
     * @return the ip by interface name
     */
    public static String getIpByInterfaceName(String interfaceName) {
        try {
            // 通过网卡名称获取 NetworkInterface
            NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
            if (networkInterface != null) {
                // 获取网卡的 IP 地址
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // 过滤掉 IPv6 地址
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取本机eth0 IP失败：{}" , e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

