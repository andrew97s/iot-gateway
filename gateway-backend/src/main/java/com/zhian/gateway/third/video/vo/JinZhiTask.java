package com.zhian.gateway.third.video.vo;

import lombok.Data;

@Data
public class JinZhiTask {
    private String taskID;
    private String name;
    private JinZhiDevice  device;
    private JinZhiStatus status;
}
