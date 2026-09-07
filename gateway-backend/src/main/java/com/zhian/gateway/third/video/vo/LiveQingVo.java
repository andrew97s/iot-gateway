package com.zhian.gateway.third.video.vo;

import lombok.Data;

import java.util.Map;

@Data
public class LiveQingVo {
    private Map<String, String> Header;
    private Map<String, String> Body;
}
