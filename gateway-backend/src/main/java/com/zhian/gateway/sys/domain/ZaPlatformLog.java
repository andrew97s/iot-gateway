package com.zhian.gateway.sys.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhian.gateway.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件运行日志 za_platform_log
 *
 * @author tongwenjin
 * @since 2026/5/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("za_platform_log")
public class ZaPlatformLog extends BaseEntity {

    /** 插件启动 */
    public static final int TYPE_START = 1;
    /** 插件停止 */
    public static final int TYPE_STOP = 2;
    /** 参数或配置更新 */
    public static final int TYPE_CONFIG = 3;
    /** 运行异常 */
    public static final int TYPE_ERROR = 4;
    /** 其它说明 */
    public static final int TYPE_OTHER = 5;

    private Long platformId;

    private Integer type;

    private String title;

    private String content;

    /** 查询用：平台代码（非表字段） */
    @TableField(exist = false)
    private String platformCode;
}
