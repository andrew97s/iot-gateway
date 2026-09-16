package com.zhian.gateway.third.video.jinzhi.vo;

import com.zhian.gateway.third.video.vo.VideoRecord;
import lombok.Data;

/**
 * 录像查询结果
 */
@Data
public class JinzhiRecord {
    private String result;
    private String msg;
    private VideoRecord[] list;

    public boolean success(){
        return result.equalsIgnoreCase("success");
    }
}
