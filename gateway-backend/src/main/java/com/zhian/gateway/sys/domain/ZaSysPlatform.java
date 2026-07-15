package com.zhian.gateway.sys.domain;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.annotation.Excel;
import com.zhian.gateway.common.core.domain.BaseEntity;
import com.zhian.gateway.common.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 平台信息对象 za_sys_platform
 *
 * @author yepanpan
 * @date 2024 -04-11
 */
@ApiModel(value = "ZaSysPlatform", description = "平台信息")
@Data
public class ZaSysPlatform extends BaseEntity {
    /**
     * The constant STATE_RUNNING.
     */
    public static final String STATE_RUNNING = "1";
    /**
     * The constant STATE_STOP.
     */
    public static final String STATE_STOP = "0";
    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * 自增长主键
     */
    @ApiModelProperty("${comment}")
    private Long id;

    /**
     * 名称
     */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /**
     * 代码
     */
    @Excel(name = "代码")
    @ApiModelProperty("代码")
    private String code;

    /**
     * IP
     */
    @Excel(name = "IP")
    @ApiModelProperty("IP")
    private String ip;

    /**
     * 端口
     */
    @Excel(name = "端口")
    @ApiModelProperty("端口")
    private Integer port;

    /**
     * 心跳接口
     */
    @ApiModelProperty("端口")
    private String apis;

    /**
     * 配置
     */
    @ApiModelProperty("端口")
    private String config;

    /**
     * 状态
     */
    @Excel(name = "状态", dictType = "sys_status")
    @ApiModelProperty("状态")
    private String status;

    /**
     * 当前运行状态
     */
    @ApiModelProperty("运行状态")
    private String running;

    /**
     * The Config object.
     */
    private JSONObject configObject;

    /**
     * Sets config.
     *
     * @param config the config
     */
    public void setConfig(String config) {
        this.config = config;
        if (StringUtils.isNotEmpty(config)) {
            configObject = JSONObject.parseObject(config);
        }
    }

    /**
     * Gets config str.
     *
     * @param key the key
     * @return the config str
     */
    public String getConfigStr(String key) {
        if (configObject == null) {
            return null;
        }

        return configObject.getString(key);
    }

    /**
     * Gets config str.
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the config str
     */
    public String getConfigStr(String key, String defaultValue) {
        if (configObject == null) {
            return defaultValue;
        }

        String value = configObject.getString(key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    /**
     * Gets config int.
     *
     * @param key the key
     * @return the config int
     */
    public Integer getConfigInt(String key) {
        if (configObject == null) {
            return null;
        }

        return configObject.getInteger(key);
    }

    /**
     * Gets config boolean.
     *
     * @param key the key
     * @return the config boolean
     */
    public Boolean getConfigBoolean(String key) {
        if (configObject == null) {
            return null;
        }

        return configObject.getBoolean(key);
    }
}
