package com.zhian.gateway.third.video.lt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拨号发起请求工具类：
 *  url：http://10.215.136.74:8086/esdk/rest/ec/appserver/ctd
 *  username：esdk_user
 *  password：Ecidi@99
 *  TYEP:POST
 * Http Digest Request contains POST、GET、PUT
 */
public class DigestRequest {

	private static final Logger logger = LoggerFactory.getLogger(DigestRequest.class);
	
    public static void main(String[] args) {
        //curl --digest --user esdk_user:Ecidi@99  -d "{\"caller\":\"667218\",\"callee\":\"26052\",\"account\":\"qiao_zy\"}" "http://10.215.136.74:8086/esdk/rest/ec/appserver/ctd"
        String url = "http://192.168.0.17";
        String account = "admin";
        String password = "admin";
        String token = login(url+"/SysUserInf/Login", account, password);
        if(token == null) {
        	logger.error("not get digest");
        	return;
        }

        String json = post(url+"/GetDeviceList",  account, password, "{}", token);	        	
        
        System.out.println(json);
    }

    static int nc = 0;    //调用次数
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    
    /**
     * 登录认证
     * @param uri
     * @param account
     * @param password
     * @return
     */
    public static String login(String uri, String account, String password) {
        String json = "UserAcct="+account+"&UserPwd="+password+"&SessionToken";
        BufferedReader in = null;
        try {
            String wwwAuth = sendGet(uri, null);       //发起一次授权请求
            if (wwwAuth.startsWith("LTWWW-Authenticate:")) {
                wwwAuth = wwwAuth.replaceFirst("LTWWW-Authenticate:", "");
                logger.info("get digest: {}", wwwAuth);
            } else {
                return null;
            }
            nc ++;
            URL realUrl = new URL(uri);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();

            // 设置是否向connection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true
            connection.setDoOutput(true);
            // Read from the connection. Defaultis true.
            connection.setDoInput(true);
            // 默认是 GET方式
            connection.setRequestMethod(POST);
            // Post 请求不能使用缓存
            connection.setUseCaches(false);

            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth,realUrl, account, password, POST, "url");

            if (!StringUtils.isEmpty(json)) {
                byte[] writebytes =json.getBytes();
                connection.setRequestProperty("Content-Length",String.valueOf(writebytes.length));
                OutputStream outwritestream = connection.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }
            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {
                return wwwAuth;
            } else {
                String errResult = formatResultInfo(connection, "json");
                logger.info(errResult);
                return null;
            }
        } catch (Exception e) {
            nc = 0;
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        
    }
    

    /**
     * 发起Post请求
     * @param uri
     * @param account
     * @param password
     * @param json
     * @param wwwAuth
     * @return
     */
    public static String post(String uri, String account, String password, String json, String wwwAuth) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
        	logger.debug("post to {}, params: {}", uri, json);
            URL realUrl = new URL(uri);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();

            // 设置是否向connection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true
            connection.setDoOutput(true);
            // Read from the connection. Defaultis true.
            connection.setDoInput(true);
            // 默认是 GET方式
            connection.setRequestMethod(POST);
            // Post 请求不能使用缓存
            connection.setUseCaches(false);

            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth,realUrl, account, password, POST, "url");

            if (!StringUtils.isEmpty(json)) {
                byte[] writebytes =json.getBytes();
                connection.setRequestProperty("Content-Length",String.valueOf(writebytes.length));
                OutputStream outwritestream = connection.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }
            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            } else {
                String errResult = formatResultInfo(connection, "json");
                logger.info(errResult);
                return errResult;
            }
        } catch (Exception e) {
            nc = 0;
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        
    }

    /**
     * 向指定URL发送DELETE方法的请求
     * @param url                                   发送请求的URL
     * @param param                                 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param username                              验证所需的用户名
     * @param password                              验证所需的密码
     * @param json                                  请求json字符串
     * @param type                                  返回xml和json格式数据，默认xml，传入json返回json数据
     * @return URL                                  所代表远程资源的响应结果
     */
     public static String sendDelete(String url, String param, String username, String password, String json, String type) {

        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String wwwAuth = sendGet(url, param);       //发起一次授权请求
            if (wwwAuth.startsWith("WWW-Authenticate:")) {
                wwwAuth = wwwAuth.replaceFirst("WWW-Authenticate:", "");
            } else {
                return wwwAuth;
            }
            nc ++;
            String urlNameString = url + (StringUtils.isNotEmpty(param) ? "?" + param : "");
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();

            // 设置是否向connection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true
            connection.setDoOutput(true);
            // Read from the connection. Defaultis true.
            connection.setDoInput(true);
            // 默认是 GET方式
            connection.setRequestMethod(DELETE);

            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth, realUrl, username, password, DELETE, type);

            if (!StringUtils.isEmpty(json)) {
                byte[] writebytes =json.getBytes();
                connection.setRequestProperty("Content-Length",String.valueOf(writebytes.length));
                OutputStream outwritestream = connection.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }

            if (connection.getResponseCode() == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            } else {
                String errResult = formatResultInfo(connection, type);
                logger.info(errResult);
                return errResult;
            }

            nc = 0;
        } catch (Exception e) {
            nc = 0;
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }

 

    /**
     * 向指定URL发送PUT方法的请求
     * @param url                                      发送请求的URL
     * @param param                                    请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param username                                 验证所需的用户名
     * @param password                                 验证所需的密码
     * @param json                                     请求json字符串
     * @param type                                     返回xml和json格式数据，默认xml，传入json返回json数据
     * @return URL                                     所代表远程资源的响应结果
     */
    public static String sendPUT(String url, String param, String username, String password, String json, String type) {

        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String wwwAuth = sendGet(url, param);       //发起一次授权请求
            if (wwwAuth.startsWith("WWW-Authenticate:")) {
                wwwAuth = wwwAuth.replaceFirst("WWW-Authenticate:", "");
            } else {
                return wwwAuth;
            }
            nc ++;
            String urlNameString = url + (StringUtils.isNotEmpty(param) ? "?" + param : "");
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();

            // 设置是否向connection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true
            connection.setDoOutput(true);
            // Read from the connection. Defaultis true.
            connection.setDoInput(true);
            // 默认是 GET方式
            connection.setRequestMethod(PUT);
            // Post 请求不能使用缓存
            connection.setUseCaches(false);

            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth,realUrl, username, password, PUT, type);

            if (!StringUtils.isEmpty(json)) {
                byte[] writebytes =json.getBytes();
                connection.setRequestProperty("Content-Length",String.valueOf(writebytes.length));
                OutputStream outwritestream = connection.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }

            if (connection.getResponseCode() == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            } else {
                String errResult = formatResultInfo(connection, type);
                logger.info(errResult);
                return errResult;
            }

            nc = 0;
        } catch (Exception e) {
            nc = 0;
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }

    /**
     * 向指定URL发送POST方法的请求
     * @param url                                       发送请求的URL
     * @param param                                     请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param username                                  验证所需的用户名
     * @param password                                  验证所需的密码
     * @param json                                      请求json字符串
     * @param type                                      返回xml和json格式数据，默认xml，传入json返回json数据
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param, String username, String password, String json, String type) {

        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String wwwAuth = sendGet(url, param);       //发起一次授权请求
            if (wwwAuth.startsWith("LTWWW-Authenticate:")) {
                wwwAuth = wwwAuth.replaceFirst("LTWWW-Authenticate:", "");
            } else {
                return wwwAuth;
            }
            nc ++;
            String urlNameString = url + (StringUtils.isNotEmpty(param) ? "?" + param : "");
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();

            // 设置是否向connection输出，因为这个是post请求，参数要放在
            // http正文内，因此需要设为true
            connection.setDoOutput(true);
            // Read from the connection. Defaultis true.
            connection.setDoInput(true);
            // 默认是 GET方式
            connection.setRequestMethod(POST);
            // Post 请求不能使用缓存
            connection.setUseCaches(false);

            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth,realUrl, username, password, POST, type);

            if (!StringUtils.isEmpty(json)) {
                byte[] writebytes =json.getBytes();
                connection.setRequestProperty("Content-Length",String.valueOf(writebytes.length));
                OutputStream outwritestream = connection.getOutputStream();
                outwritestream.write(json.getBytes());
                outwritestream.flush();
                outwritestream.close();
            }
            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
            } else {
                String errResult = formatResultInfo(connection, type);
                logger.info(errResult);
                return errResult;
            }

            nc = 0;
        } catch (Exception e) {
            nc = 0;
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }


    /**
     * 向指定URL发送GET方法的请求
     * @param url                                       发送请求的URL
     * @param param                                     请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param username                                  验证所需的用户名
     * @param password                                  验证所需的密码
     * @param type                                      返回xml和json格式数据，默认xml，传入json返回json数据
     * @return URL 所代表远程资源的响应结果
     */  
    public static String sendGet(String url, String param, String username, String password, String type) {
 
        StringBuilder result = new StringBuilder();  
        BufferedReader in = null;  
        try {  
            String wwwAuth = sendGet(url, param);       //发起一次授权请求
            if (wwwAuth.startsWith("WWW-Authenticate:")) {  
                wwwAuth = wwwAuth.replaceFirst("WWW-Authenticate:", "");  
            } else {  
                return wwwAuth;  
            }  
            nc ++;
            String urlNameString = url + (StringUtils.isNotEmpty(param) ? "?" + param : "");
            URL realUrl = new URL(urlNameString);  
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            // 设置通用的请求属性
            setRequestProperty(connection, wwwAuth,realUrl, username, password, GET, type);
            // 建立实际的连接
            // connection.connect();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;  
            while ((line = in.readLine()) != null) {  
                result.append(line);  
            }  
            nc = 0;
        } catch (Exception e) {  
            nc = 0;  
            throw new RuntimeException(e);  
        } finally {  
            try {  
                if (in != null) in.close();  
            } catch (Exception e2) {  
                e2.printStackTrace();  
            }  
        }  
        return result.toString();  
    }  
 
    /**  
     * 生成授权信息  
     * @param authorization                             上一次调用返回401的WWW-Authenticate数据
     * @param username                                  用户名
     * @param password                                  密码
     * @return                                          授权后的数据, 应放在http头的Authorization里
     * @throws IOException                              异常
     */  
    private static String getAuthorization(String authorization, String uri, String username, String password, String method) throws IOException {
 
        uri = StringUtils.isEmpty(uri) ? "/" : uri;  
        // String temp = authorization.replaceFirst("Digest", "").trim();
        String temp = authorization.replaceFirst("Digest", "").trim().replace("MD5","\"MD5\"");
        // String json = "{\"" + temp.replaceAll("=", "\":").replaceAll(",", ",\"") + "}";
        String json = withdrawJson(authorization);
        // String json = "{ \"realm\": \"Wowza\", \" domain\": \"/\", \" nonce\": \"MTU1NzgxMTU1NzQ4MDo2NzI3MWYxZTZkYjBiMjQ2ZGRjYTQ3ZjNiOTM2YjJjZA==\", \" algorithm\": \"MD5\", \" qop\": \"auth\" }";

        JSONObject jsonObject = JSON.parseObject(json);  
        String cnonce = DigestUtil.generateSalt2(8);
        String ncstr = ("00000000" + nc).substring(Integer.toString(nc).length());     //认证的次数,第一次是1，第二次是2...  
        // String algorithm = jsonObject.getString("algorithm");
        String algorithm = jsonObject.getString("algorithm");
        String qop = jsonObject.getString("qop");
        String nonce = jsonObject.getString("nonce");
        String realm = jsonObject.getString("realm");

        String response = DigestUtil.http_da_calc_HA1(username, realm, password,
                nonce, ncstr, cnonce, qop,
                method, uri, algorithm);
 
        //组成响应authorization  
        authorization = "Digest username=\"" + username + "\"," + temp;  
        authorization += ",uri=\"" + uri  
                + "\",nc=\"" + ncstr  
                + "\",cnonce=\"" + cnonce  
                + "\",response=\"" + response+"\"";  
        return authorization;  
    }

    /**
     * 将返回的Authrization信息转成json
     * @param authorization                                     authorization info
     * @return                                                  返回authrization json格式数据 如：String json = "{ \"realm\": \"Wowza\", \" domain\": \"/\", \" nonce\": \"MTU1NzgxMTU1NzQ4MDo2NzI3MWYxZTZkYjBiMjQ2ZGRjYTQ3ZjNiOTM2YjJjZA==\", \" algorithm\": \"MD5\", \" qop\": \"auth\" }";
     */
    private static String withdrawJson(String authorization) {
        String temp = authorization.replaceFirst("Digest", "").trim().replaceAll("\"","");
        // String noncetemp = temp.substring(temp.indexOf("nonce="), temp.indexOf("uri="));
        // String json = "{\"" + temp.replaceAll("=", "\":").replaceAll(",", ",\"") + "}";
        String[] split = temp.split(",");
        Map<String, String> map = new HashMap<>();
        Arrays.asList(split).forEach(c -> {
            String c1 = c.replaceFirst("=",":");
            String[] split1 = c1.split(":");
            map.put(split1[0].trim(), split1[1].trim());
        });
        return JSONObject.toJSONString(map);
    }

    /**
     * 向指定URL发送GET方法的请求
     * @param url                                                   发送请求的URL
     * @param param                                                 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL                                                  所代表远程资源的响应结果
     */  
    public static String sendGet(String url, String param) {  
        StringBuilder result = new StringBuilder();  
        BufferedReader in = null;  
        try {  
 
            String urlNameString = url + (StringUtils.isNotEmpty(param) ? "?" + param : "");
            URL realUrl = new URL(urlNameString);  
            // 打开和URL之间的连接  
            URLConnection connection = realUrl.openConnection();  
            // 设置通用的请求属性  
            connection.setRequestProperty("accept", "*/*");  
            connection.setRequestProperty("connection", "Keep-Alive");  
            connection.setRequestProperty("user-agent",  
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");  
 
            connection.connect();  
 
            //返回401时需再次用用户名和密码请求  
            //此情况返回服务器的 WWW-Authenticate 信息  
            if (((HttpURLConnection) connection).getResponseCode() == 401) {  
                Map<String, List<String>> map = connection.getHeaderFields();  
                String header = "LTWWW-Authenticate:" + map.get("LTWWW-Authenticate").get(0);  
                logger.info("author header: {}", header);
                return header;
            }  
 
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));  
            String line;  
            while ((line = in.readLine()) != null) {  
                result.append(line);  
            }  
        } catch (Exception e) {  
            throw new RuntimeException("get请求发送失败",e);  
        }  
        // 使用finally块来关闭输入流  
        finally {  
            try {  
                if (in != null) in.close();  
            } catch (Exception e2) {  
                e2.printStackTrace();  
            }  
        }  
        return result.toString();  
    }

    /**
     * HTTP set request property
     *
     * @param connection                            HttpConnection
     * @param wwwAuth                               授权auth
     * @param realUrl                               实际url
     * @param username                              验证所需的用户名
     * @param password                              验证所需的密码
     * @param method                                请求方式
     * @param type                                  返回xml和json格式数据，默认xml，传入json返回json数据
     */
    private static void setRequestProperty(HttpURLConnection connection, String wwwAuth, URL realUrl, String username, String password, String method, String type)
        throws IOException {

    	if(type == null) {
    		 // 返回xml
            if (!method.equals(GET)) {
                connection.setRequestProperty("Content-Type","application/json;charset=UTF-8");
            }
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            // connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("user-agent",
                                          "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
    	}else if(type.equals("json")) {
            // 返回json
            connection.setRequestProperty("accept", "application/json;charset=UTF-8");
            connection.setRequestProperty("Content-Type","application/json;charset=UTF-8");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                                          "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        } else if(type.equals("xml")){

            connection.setRequestProperty("accept", "application/json;charset=UTF-8");
            connection.setRequestProperty("Content-Type","application/xml;charset=UTF-8");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                                          "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

        } else if(type.equals("url")){

            connection.setRequestProperty("accept", "application/json;charset=UTF-8");
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                                          "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");

         }
        //授权信息
        String authentication = getAuthorization(wwwAuth, realUrl.getPath(), username, password, method);
        connection.setRequestProperty("Authorization", authentication);
    }

    /**
     * 格式化请求返回信息，支持json和xml格式
     * @param connection                                HttpConnection
     * @param type                                      指定返回数据格式，json、xml，默认xml
     * @return                                          返回数据
     */
    private static String formatResultInfo(HttpURLConnection connection, String type) throws IOException {
        String result = "";
        if (type != null && type.equals("json")) {
            result = String.format("{\"errCode\":%s, \"message\":%s}",connection.getResponseCode(),connection.getResponseMessage());
        } else {
            result = String.format(" <?xml version=\"1.0\" encoding=\"UTF-8\" ?> "
                                       + " <wmsResponse>"
                                       + " <errCode>%d</errCode>"
                                       + " <message>%s</message>"
                                       + " </wmsResponse>",connection.getResponseCode(),connection.getResponseMessage());
        }
        return result;
    }

}