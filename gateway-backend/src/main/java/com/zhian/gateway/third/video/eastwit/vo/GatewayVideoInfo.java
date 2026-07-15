package com.zhian.gateway.third.video.eastwit.vo;

import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.third.common.constants.DeviceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 视频网关-摄像头信息
 *
 * @author yangyixin
 */
@Data
public class GatewayVideoInfo {

    private GatewayInfo gatewayInfo;

    private List<DeviceInfo> devlist;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class GatewayInfo {
        private String gatewayId;
        private String gatewayName;
        private String gatewayIp;
        private String gatewayPort;
        private String gatewayPlayUrl;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DeviceInfo {
        private String devId;
        private String devCode;
        private String chnNo;
        private String devIp;
        private String devName;
        private String devPort;
        private String devUser;
        private String devPsw;
        private String protocol;
        private String url;
        private String devNo;
        private String bitstream;
        private Integer online; // 在线状态 0离线 1在线
        private Integer status; // 设备连接状态 0未配置 1配置错误 2正常
        private PlayUrl playurl;

        @Data
        @EqualsAndHashCode(callSuper = false)
        public static class PlayUrl {
            private String flv;
            private String hls;
            private String rtmp;
            private String rtsp;
            private String ws;
        }


        public ZaSysDevice toCamera(){
            ZaSysDevice camera = new ZaSysDevice();
            camera.setCode(devId);
            camera.setBizId(devId);
            camera.setType(DeviceType.CAMERA);
            camera.setIp(devIp);
            camera.setName(devName);
            camera.setWireless("0");
            camera.setOnline(online+"");
            camera.setRemark(JSONObject.toJSONString(playurl));
            return camera;
        }
    }

}
