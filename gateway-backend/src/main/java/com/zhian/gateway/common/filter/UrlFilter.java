package com.zhian.gateway.common.filter;


import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 拦截请求中包含的特殊字符
 * @author lsh
 * @version 1.0
 * @date 2022/4/1 16:27
 */

public class UrlFilter {
    /**
     * 特殊字符正则表达式
     */

//    private final static String REG_EX = "[\\x{1F600}-\\x{1F64F}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F300}-\\x{1F5FF}\\x{1F900}-\\x{1F9FF}\\x{1F1E0}-\\x{1F1FF}]";
    private final static String REG_EX = "[\\uD83C\\uDF00-\\uD83D\\uDDFF\\uD83D\\uDE00-\\uD83D\\uDEFF\\uD83E\\uDD00-\\uD83E\\uDDFF\\uD83C\\uDC00-\\uD83C\\uDFFF]";
//    private final static String REG_EX_EMOJI = "[\\uD83C\\uDF00-\\uD83D\\uDDFF\\uD83D\\uDE00-\\uD83D\\uDEFF\\uD83E\\uDD00-\\uD83E\\uDDFF\\uD83C\\uDC00-\\uD83C\\uDFFF]";

    /**
     * 判断url中是否含有特殊字符
     * @param urls 前端请求链接
     * @return 是否包含特殊字符
     */
    public static boolean checkSpecials(String urls) {
        try {
            if (StringUtils.isNotEmpty(urls)) {
                // url参数转义
                urls = URLDecoder.decode(urls, "utf-8");
                if (Pattern.compile(REG_EX).matcher(urls).find()) {
                    return true;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断formData值对象中是否包含特殊字符
     * @param map formData值对象
     * @return 是否包含特殊字符
     */
    public static boolean checkSpecials(Map<String,String[]> map){
        if(!map.isEmpty()){
            for(String[] paraArray : map.values()){
                for(String paraStr : paraArray){
                    if(Pattern.compile(REG_EX).matcher(paraStr).find()){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断前端传过来的json和json数组中是否含有特殊字符
     * @param request 前端请求(包含json数据)
     * @return 是否包含特殊字符
     */
    public static boolean checkSpecials(HttpServletRequest request) {
        try {
            SpecialCharHttpServletRequestWrapper wrapper = new SpecialCharHttpServletRequestWrapper(request);
            InputStream is = wrapper.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() > 0) {
                //判断json是否包含list数组，包含则先遍历数组再遍历对象值
                if (sb.toString().contains("[")){
                    List<Object> objectList = JSONObject.parseObject(sb.toString(), new TypeReference<List<Object>>() {});
                    for(Object objTemp:objectList){
                        Map<String, Object> map = JSONObject.parseObject(JSONObject.parseObject(objTemp.toString()).toJSONString(), new TypeReference<Map<String, Object>>() {});
                        for (Object object : map.values()) {
                            if (object != null) {
                                Matcher m = Pattern.compile(REG_EX).matcher(object.toString());
                                if (m.find()) {
                                    return true;
                                }
                            }
                        }
                    }
                }else{
                    Map<String, Object> map = JSONObject.parseObject(JSONObject.parseObject(sb.toString()).toJSONString(), new TypeReference<Map<String, Object>>() {});
                    for (Object object : map.values()) {
                        if (object != null) {
                            Matcher m = Pattern.compile(REG_EX).matcher(object.toString());
                            if (m.find()) {
                                return true;
                            }
                        }

                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
