package com.zhian.gateway.third.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhian.gateway.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.Date;

/**
 * 门禁出入对象 za_safe_door
 * 
 * @author yepanpan
 * @date 2025-06-04
 */
@Data
public class DoorIoMsg extends BaseEntity
{

    /** 消息代码 */
    private String msgId;

    /** 出入类型 */
    private String type;

    /** 出入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date ioTime;

    /** 门禁名称 */
    private String doorName;

    /** 人脸图片 */
    private String imgFile;

    /** 姓名 */
    private String name;

    /** 身份证id */
    private String idnum;

    /** 用户类型 */
    private String userType;

    /** 人员编号 */
    private String personNo;



}
