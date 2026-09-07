package com.zhian.gateway.third.dahua.vo;

import com.dahuatech.icc.oauth.http.IccResponse;
import lombok.Data;

import java.util.List;

/**
 * ICC平台摄像机分页查询返回
 */
@Data
public class IccCameraResponse extends IccResponse {

    private CameraData data;

    @Data
    public static class CameraData {
        List<DeviceInfo> pageData;
        Integer currentPage;
        Integer totalPage;
        Integer pageSize;
        Integer totalRows;
    }

    @Data
    public static class DeviceInfo{
        String deviceCode;
        String deviceName;
        String deviceSn;
        String deviceManufacturer;
        String deviceModel;
        Integer deviceCategory;
        String deviceType;
        String deviceIp;
        Integer devicePort;
        String ownerCode;
        String isOnline;
        String gpsX;
        String gpsY;
        String offlineReason;
        String subSystem;
        List<UnitsInfo> units;
    }

    @Data
    public static class UnitsInfo{
        Integer unitType;
        Integer unitSeq;
        String capability;
        String memo;
        String unitExt;
        List<CameraChannel> channels;
    }

    @Data
    public static class CameraChannel{
        String channelName;
        Integer channelSeq;
        String channelCode;
        String channelSn;
        String channelType;
        String cameraType;
        String capability;
        String gpsX;
        String gpsY;
        String gpsZ;
        String ownerCode;
        Integer isOnline;
        Integer sleepStat;
        String domainId;
        String memo;
        Integer stat;
    }
}
