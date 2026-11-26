package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 查询设备列表响应
 *
 * @author tongwenjin
 * @since 2024-11-26
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchDeviceResp extends EzvizResp{

    private List<DeviceData> data;

    private PageData page;


    @Data
    public static class DeviceData {
        private String id;

        private String deviceSerial;

        private String deviceName;

        private String deviceType;

        // 设备在线状态，1-在线；0-离线
        private Integer status;

        // 布撤防状态
        private Integer defense;

        // 固件版本号
        private String deviceVersion;

        private Long addTime;

        private Long updateTime;

        private String parentCategory;

        // 设备风险安全等级，0-安全；大于0，有风险，风险越高，值越大
        private Integer riskLevel;

        // 设备IP地址
        private String netAddress;
    }

    @Data
    public static class PageData {

        private Integer total;

        private Integer page;

        private Integer size;
    }
}
