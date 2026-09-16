package com.zhian.gateway.framework.license.vo;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 证书信息
 *
 * @author tongwenjin
 * @since 2024-9-3
 */

@Data
@ApiModel("证书信息")
public class LicenseInfo {

    @ApiModelProperty("证书描述")
    private String description;

    @ApiModelProperty("证书过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    @ApiModelProperty("证书生效时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date issueTime;

    @ApiModelProperty("剩余可用天数")
    private Long validDays;

    public Long getValidDays() {
        long days = DateUtil.betweenDay(expireTime, new Date(), true);
        long ms = DateUtil.betweenMs(expireTime, new Date());
        // 剩余可用天数 > 3650天 记作永久授权
        days = days > 3650 ? -1 : days;
        // 剩余可用天数 < 1天 > 1毫秒 记作1天
        return days == 0 ? (ms > 1 ? 1 : 0) : days;
    }
}
