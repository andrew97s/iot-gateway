package com.zhian.gateway.third.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 道闸出入对象 za_safe_gate
 * 
 * @author yepanpan
 * @date 2024-12-04
 */
@Data
public class CarIoMsg extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息代码 */
    private String msgId;

    /** 出入类型 */
    private String type;

    /** 出入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date ioTime;

    /** 车牌号 */
    private String plate;

    /** 道闸名称 */
    private String gateName;

    /** 车辆图片 */
    private String imgFile;
}
