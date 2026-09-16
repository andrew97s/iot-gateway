package com.zhian.gateway.framework.web.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * websocket 配置
 * 
 * @author ruoyi
 */
@Configuration
public class WebSocketConfig
{
    @Bean
    public ServerEndpointExporter serverEndpointExporter()
    {
        return new ServerEndpointExporter();
    }

    /**
     * WebSocket 配置信息
     *
     * @return servletServerContainerFactoryBean
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean bean = new ServletServerContainerFactoryBean();

        // 文本缓冲区大小
        bean.setMaxTextMessageBufferSize(1024*20);
        // 字节缓冲区大小
        bean.setMaxBinaryMessageBufferSize(1024*20);

        return bean;

    }
}
