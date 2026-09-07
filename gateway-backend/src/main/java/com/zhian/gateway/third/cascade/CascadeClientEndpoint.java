package com.zhian.gateway.third.cascade;

import com.zhian.gateway.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 级联控制器
 */
@ClientEndpoint
@Slf4j
public class CascadeClientEndpoint {
    @OnOpen
    public void onOpen(Session session) {
        log.info("级联端连接已经打开: {}", session.getId());

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("级联端连接已经断开 {} reason {}", session.getId(), closeReason);
    }


    @OnMessage
    public void onMessage(Session session, String msg) {
        log.info("级联端接收到上级下发的消息: {}", msg);
        SpringUtils.getBean(CascadeHandler.class).processMsg(msg);
    }
    @OnError
    public void onError(Session session, Throwable t) {
        log.error("级联端连接发生错误{} to {}", session.getId(), t);
    }

    /**
     * 忽略证书
     * @param
     */
    public static void trustAllHosts(){
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){
                return new X509Certificate[]{};
            }
            @Override
            public void checkClientTrusted(X509Certificate[] arg0,String arg1)throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1)throws CertificateException {
            }
        }};
        try{
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trustAllCerts,new java.security.SecureRandom());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
