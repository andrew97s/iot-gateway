package com.zhian.gateway.sys.domain;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.common.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 上级平台连接配置 za_sys_upstream
 *
 * 支持多上级平台同时接入；每个平台一条记录，推送方式支持：
 * url（HTTP/HTTPS 直推）、redis（Redis 队列）、mq（RabbitMQ 消息队列）。
 */
@ApiModel(value = "ZaSysUpstream", description = "上级平台连接配置")
@Data
@TableName("za_sys_upstream")
public class ZaSysUpstream implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PUSH_TYPE_URL = "url";
    public static final String PUSH_TYPE_REDIS = "redis";
    public static final String PUSH_TYPE_MQ = "mq";

    public static final String STATUS_ENABLED = "1";

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键")
    private Long id;

    /** 平台名称 */
    @ApiModelProperty("平台名称")
    private String name;

    /** 平台代码（唯一） */
    @ApiModelProperty("平台代码")
    private String code;

    /** 推送方式：url / redis / mq */
    @ApiModelProperty("推送方式")
    private String pushType;

    /**
     * 连接配置 JSON，按推送方式取字段：
     * url:   {"pushUrls":"http://a\nhttp://b"}
     * redis: {"ip":"","port":"6379","password":"","db":"6"}
     * mq:    {"ip":"","port":5672,"vhost":"/","username":"","password":"","exchange":"za","queue":"za_monitor","key":"za"}
     */
    @ApiModelProperty("连接配置(JSON)")
    private String config;

    /** 状态：1启用（消息同步到该平台） 0停用 */
    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 配置对象（非表字段） */
    @TableField(exist = false)
    private JSONObject configObject;

    public JSONObject configObject() {
        if (configObject == null && StringUtils.isNotEmpty(config)) {
            configObject = JSONObject.parseObject(config);
        }
        return configObject == null ? new JSONObject() : configObject;
    }

    /** 转换为发送器所需的 ZaSysPlatform 配置载体（复用既有 MessageSender 实现） */
    public ZaSysPlatform toPlatformConfig() {
        ZaSysPlatform platform = new ZaSysPlatform();
        platform.setId(id);
        platform.setName(name);
        platform.setCode(code);
        platform.setStatus(status);
        if (StringUtils.isNotEmpty(config)) {
            platform.setConfig(config);
        }
        return platform;
    }

    /** 推送目标摘要（用于展示与推送记录） */
    public String targetSummary() {
        JSONObject c = configObject();
        if (PUSH_TYPE_URL.equals(pushType)) {
            String urls = c.getString("pushUrls");
            if (StringUtils.isEmpty(urls)) {
                urls = c.getString("pushUrl");
            }
            return StringUtils.isEmpty(urls) ? "(未配置推送URL)" : urls.replace('\n', ';');
        }
        if (PUSH_TYPE_MQ.equals(pushType)) {
            return String.format("RabbitMQ %s:%s vhost=%s exchange=%s queue=%s key=%s",
                    c.getString("ip"), c.getString("port"), c.getOrDefault("vhost", "/"),
                    c.getString("exchange"), c.getString("queue"), c.getString("key"));
        }
        if (PUSH_TYPE_REDIS.equals(pushType)) {
            return String.format("Redis %s:%s db=%s",
                    c.getOrDefault("ip", "127.0.0.1"), c.getOrDefault("port", "6379"), c.getOrDefault("db", "6"));
        }
        return pushType;
    }
}
