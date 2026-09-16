package com.zhian.gateway.third.dahua.vo;

import com.alibaba.fastjson2.JSONObject;
import com.dahuatech.icc.oauth.http.IccResponse;
import lombok.Data;

/**
 * ICC平台截图返回结果
 * {
 *     "code": 1000,
 *     "desc": "Success",
 *     "data": "{\n   \"id\" : 123,\n   \"method\" : \"dev.snap\",\n   \"params\" : {\n      \"DevChannel\" : 0,\n      \"DevID\" : \"1035608\",\n      \"PicInfo\" : \"lihuiqin1_SF_15/archivefile1-2021-04-02-020901-F7ED443C500FDD5A:0/289695.jpg\",\n      \"PicInfoType\" : 1,\n      \"cmdSource\" : 0\n   },\n   \"result\" : true\n}\n"
 * }
 */
@Data
public class IccSnapResponse extends IccResponse {
    private String data;

    private JSONObject params;
    public String getImg(){
        if(!isSuccess()){
            return null;
        }

        if(params == null) {
            params = JSONObject.parseObject(data);
        }
        return params.getJSONObject("params").getString("PicInfo");
    }
}
