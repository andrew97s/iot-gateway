package com.zhian.gateway.third.video.roc.event;

import lombok.Data;

import java.util.Date;

/**
 * Roc 人脸识别事件
 *
 * @author tongwenjin
 * @since 2025-2-20
 */

@Data
public class RocFaceEvent {

    public static final String SCAN = "scan";
    public static final String ADD = "add";
    public static final String MODIFY = "modify";
    public static final String DELETE = "delete";
    public static final String CLEAR = "clear";


    private Date time;

    /**
     * 用户ID
     */
    private String name;

    private String similarity;

    /**
     * 人脸照片 http url
     */
    private String faceImg;

    private String action;
    private String result;

    private String describe;
}
