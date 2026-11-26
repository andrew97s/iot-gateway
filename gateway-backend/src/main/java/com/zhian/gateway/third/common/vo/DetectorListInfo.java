package com.zhian.gateway.third.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 青瞳主机信息
 */
@Data
public class DetectorListInfo {
    private String deviceId;

    List<Detector> detectors;

    @Data
    public static class Detector{
        String ip;
        String model;
        String name;
        String sn;
        List<Camera> cameras;
    }

    @Data
    public static class Camera{
        String flvUrl;
        String rtspUrl;
    }
}
