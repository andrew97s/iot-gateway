package com.zhian.gateway.framework.interceptor;

import com.zhian.gateway.common.filter.UrlFilter;
import com.zhian.gateway.common.utils.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yangyixin
 */
@Component
public class SpecialCharInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if("GET".equals(httpServletRequest.getMethod())){
            if(UrlFilter.checkSpecials(httpServletRequest.getQueryString())){
                throw new Exception("url中包含特殊字符");
            }
        }else{
            String contentType = httpServletRequest.getContentType();
            //处理form-data请求类型数据值
            if (contentType != null && contentType.contains("multipart/form-data")) {
                MultipartResolver resolver = new CommonsMultipartResolver(httpServletRequest.getSession().getServletContext());
                MultipartHttpServletRequest multipartRequest = resolver.resolveMultipart(httpServletRequest);
                if(UrlFilter.checkSpecials(multipartRequest.getParameterMap())){
                    throw new Exception("请求参数中包含特殊字符");
                }
            }
            else{
//                if(UrlFilter.checkSpecials(httpServletRequest)){
//                    throw new Exception("请求的数据中包含特殊字符 ");
//                }
            }
        }
        return true;
    }

    private String filterEmoji(String str) {
        // 过滤表情符号的逻辑
        if (StringUtils.isBlank(str)) {
            return str;
        }
        System.out.println("[" + str + "]");
        return str.replaceAll("\\s+", "");
//        return str.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
    }

}
