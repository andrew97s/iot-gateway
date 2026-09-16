package com.zhian.gateway.third.vo;

import com.zhian.gateway.sys.domain.ZaSysPlatform;
import lombok.Data;

import java.util.Date;

/**
 * 待处理消息
 */
@Data
public class MessageVo {
    /**
     * 平台代码
     */
    private ZaSysPlatform platform;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 接收时间
     */
    private Date createTime;

    /**
     * 格式化的数据
     */
    private Object data;
}
