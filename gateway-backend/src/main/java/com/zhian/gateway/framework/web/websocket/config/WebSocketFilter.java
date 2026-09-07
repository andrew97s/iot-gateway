package com.zhian.gateway.framework.web.websocket.config;

import com.zhian.gateway.common.utils.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = {"/ws/*","/jz/ws","/cascade/*"})
@Order(0)
@Component
public class WebSocketFilter implements Filter{
    public static final String IP_ATTR_NAME = "client_ip";
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpSession session = req.getSession();
        String ip = ((HttpServletRequest) servletRequest).getHeader("X-Real-IP");

        session.setAttribute(IP_ATTR_NAME, StringUtils.isEmpty(ip) ?  req.getRemoteHost() : ip);//获取ip存入session
        if (StringUtils.isNotEmpty(req.getRemoteHost())) {
            filterChain.doFilter(servletRequest, servletResponse);
        }else{
            throw new ServletException("获取客户端IP失败");
        }
    }
}
