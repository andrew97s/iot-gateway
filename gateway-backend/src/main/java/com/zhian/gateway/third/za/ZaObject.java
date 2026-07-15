package com.zhian.gateway.third.za;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.framework.license.LicenseManagerHolder;
import com.zhian.gateway.framework.web.domain.Server;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import lombok.Data;

import java.util.Date;

@Data
public class ZaObject extends Server{
    private static ZaObject object = null;
    private String clientId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date licenseTo;

    public static ZaObject get(String clientId){
        if(object == null){
            object = new ZaObject();
            object.clientId = clientId;
        }
        try {
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(null);
            LicenseContent licenseContent = licenseManager.verify();
            object.licenseTo = licenseContent.getNotAfter();
            object.copyTo();
        }catch (Exception e){
            e.printStackTrace();
        }
        return object;
    }

    public String toJSON(){
        return JSONObject.toJSONString(this);
    }

    @Override
    public String toString(){
        return toJSON();
    }
}
