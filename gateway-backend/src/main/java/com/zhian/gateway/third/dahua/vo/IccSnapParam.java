package com.zhian.gateway.third.dahua.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import lombok.Data;

/**
 * ICC平台截图参数
 * {
 *     "deviceCode": "1002241",
 *     "operation": "generalJsonTransport",
 *     "params": "{\"method\":\"dev.snap\",\"id\":123,\"params\":{\"DevID\":\"1002241\",\"DevChannel\":0,\"SnapType\":1,\"CmdSrc\":0}}"
 * }
 */
@Data
public class IccSnapParam {
    private String deviceCode;
    private String operation = "generalJsonTransport";
    private String params;

    /**
     * 快速生成请求参数
     * @param deviceId
     * @param channel
     * @return
     */
    public static IccSnapParam create(String deviceId, Integer channel){
        SnapDevice device = new SnapDevice();
        device.setDevID(deviceId);
        device.setDevChannel(channel);

        SnapParams snapParams = new SnapParams();
        snapParams.setParams(device);

        IccSnapParam param = new IccSnapParam();
        param.setDeviceCode(deviceId);
        try {
            param.setParams(new ObjectMapper().writeValueAsString(snapParams));
        }catch (Exception e){
            e.printStackTrace();
        }
        return param;
    }

    @Data
    public static class SnapParams{
        private String method = "dev.snap";
        private Long id = SnowflakeIdWorker.getInstance().nextId();
        private  SnapDevice params;
    }

    @Data
    public static class SnapDevice {
        @JsonProperty("DevID")
        private String DevID;
        @JsonProperty("DevChannel")
        private Integer DevChannel=0;
        @JsonProperty("SnapType")
        private Integer SnapType = 1;
        @JsonProperty("CmdSrc")
        private Integer CmdSrc = 0;
    }
}
