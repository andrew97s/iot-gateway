package com.zhian.gateway.third.hk.ezviz.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 告警请求体
 *
 * @author tongwenjin
 * @since 2024 -11-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmBody extends WebhookReq.WebhookBody {

    /**
     * 设备序列号
     */
    private String devSerial;
    /**
     * 设备通道号
     */
    private String channel;
    /**
     * 设备通道类型：1-视频通道 2-IO通道
     */
    private String channelType;
    /**
     * 告警类型, 见 附录:萤石设备告警消息类型，附录<a href="https://open.ys7.com/help/76">萤石设备告警消息</a>
     */
    private String alarmType;
    /**
     * 设备自己生成的UUID, 用来标识唯一的告警,统一告警的开始、结束采用统一alarmId
     */
    private String alarmId;
    /**
     * 告警关联ID，由发起联动方产生，用来表示联动的关联关系
     */
    private String relationId;
    /**
     * 告警位置信息：长度不能超过80字节
     */
    private String location;
    /**
     * 告警描述，需要推送给客户的信息
     */
    private String describe;
    /**
     * 告警时间，格式： yyyy-MM-ddTHH:mm:ss
     */
    private String alarmTime;
    /**
     * 自定义协议类型，命名规则：设备型号_协议标识 如：CS-A1-32W_XX
     */
    private String customType;
    /**
     * 服务端记录的请求时间
     */
    private String requestTime;
    /**
     * 告警通道名称
     */
    private String channelName;
    /**
     * 设备加密密码
     */
    private String checksum;
    /**
     * 图片加密类型：0-不加密，1-用户加密，2-平台加密
     */
    private String crypt;
    /**
     * 报警自定义信息
     */
    private String customInfo;
    /**
     * 设备端的智能识别信息
     */
    private String intelligentData;
    /**
     * 告警图片相关信息
     */
    private List<Picture> pictureList;

    /**
     * The type Picture.
     */
    @Data
    public static class Picture {

        /**
         * 平台生成的告警Id
         */
        private String id;
        /**
         * 告警图片短地址
         */
        private String shortUrl;
        /**
         * 	告警图片URL
         */
        private String url;
    }
}
