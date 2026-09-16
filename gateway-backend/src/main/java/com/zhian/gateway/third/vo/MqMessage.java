package com.zhian.gateway.third.vo;

import com.zhian.gateway.common.constant.Constants;
import com.zhian.gateway.common.utils.uuid.SnowflakeIdWorker;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import lombok.Data;

import java.util.Date;

/**
 * 消息对象,网关处理之后，放入MQ的消息体
 *
 * @author yepanpan
 * @since 2024 /5/13
 */
@Data
public class MqMessage {
    /**
     * 告警事件
     */
    public static final String EVENT_ALARM = "alarm";

    /**
     * 巡检事件
     */
    public static final String EVENT_BUSINESS = "business";

    /**
     * 设备事件
     */
    public static final String EVENT_DEVICE = "device";

    /**
     * 级联事件
     */
    public static final String EVENT_CASCADE = "cascade";

    /**
     * 设备新增事件
     */
    public static final String DEVICE_ADD = "1";

    /**
     * 设备删除事件
     */
    public static final String DEVICE_REMOVE = "2";

    /**
     * 设备修改事件
     */
    public static final String DEVICE_UPDATE = "3";

    /**
     * 消息唯一ID
     */
    private String uuid;

    /**
     * 网关对应的设备ID
     */
    private Long deviceId;

    /**
     * 协议
     */
    private String protocol;

    /**
     * 消息类别:heart是心跳数据，alarm是告警数据,device是设备数据，cascade是级联响应
     */
    private String event;

    /**
     * 事件代码，一般就是告警代码,如果是设备消息，1是新增设备，2是删除设备
     */
    private String eventType;

    /**
     * 图片地址（http或file），多个以,分割
     * 一般作为告警图片使用
     */
    private String imageUrl;

    /**
     * 时间
     */
    private Date time;

    /**
     * The Facility.
     */
    private Facility facility;

    /**
     * 消息主体
     */
    private String msgData;

    /**
     * 设备消息
     *
     * @param deviceId the device id
     * @param protocol the protocol
     * @param facility the facility
     * @param msgData  the msg data
     * @return mq message
     */
    public static MqMessage createDevice(Long deviceId, String protocol, Facility facility, String msgData){
        MqMessage mqMessage = new MqMessage();
        mqMessage.setDeviceId(deviceId);
        mqMessage.setEvent(MqMessage.EVENT_DEVICE);
        mqMessage.setFacility(facility);
        mqMessage.setMsgData(msgData);
        mqMessage.setTime(new Date());
        mqMessage.setProtocol(protocol);
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        return mqMessage;
    }

    /**
     * 巡检数据消息
     *
     * @param deviceId the device id
     * @param protocol the protocol
     * @param facility the facility
     * @param msgData  the msg data
     * @return mq message
     */
    public static MqMessage createBusiness(Long deviceId, String protocol, Facility facility, String msgData){
        MqMessage mqMessage = new MqMessage();
        mqMessage.setDeviceId(deviceId);
        mqMessage.setEvent(MqMessage.EVENT_BUSINESS);
        mqMessage.setFacility(facility);
        mqMessage.setMsgData(msgData);
        mqMessage.setTime(new Date());
        mqMessage.setProtocol(protocol);
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        return mqMessage;
    }

    public static MqMessage createBusiness(
            ZaSysDevice device,String eventType,String protocol, String msgData, String imageUrl
    ){
        // 设备信息提取基础设备数据
        Facility facility = new Facility();
        facility.setType(device.getType());
        facility.setCode(device.getCode());
        facility.setName(device.getName());
        facility.setModel(device.getModel());
        facility.setNet(device.getNet());
        facility.setWireless(Constants.YES.equals(device.getWireless()));
        facility.setOnLine(true);

        MqMessage mqMessage = new MqMessage();
        mqMessage.setImageUrl(imageUrl);
        mqMessage.setDeviceId(device.getId());
        mqMessage.setEvent(MqMessage.EVENT_BUSINESS);
        mqMessage.setEventType(eventType);
        mqMessage.setFacility(facility);
        mqMessage.setMsgData(msgData);
        mqMessage.setTime(new Date());
        mqMessage.setProtocol(protocol);
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        return mqMessage;
    }

    /**
     * 创建设备业务消息(一般为有线设备，即设备本身无 信号强度、电量、温度监测数据)
     *
     * @param device 设备
     * @param msgData 源业务消息
     * @return the message
     */
    public static MqMessage createBusiness(
            ZaSysDevice device, String msgData
    ){
        // 设备信息提取基础设备数据
        Facility facility = new Facility();
        facility.setType(device.getType());
        facility.setCode(device.getCode());
        facility.setName(device.getName());
        facility.setModel(device.getModel());
        facility.setNet(device.getNet());
        facility.setWireless(Constants.YES.equals(device.getWireless()));
        facility.setOnLine(true);

        // 消息基础信息填充
        MqMessage mqMessage = new MqMessage();
        mqMessage.setFacility(facility);
        mqMessage.setDeviceId(device.getId());
        mqMessage.setEvent(MqMessage.EVENT_BUSINESS);
        mqMessage.setMsgData(msgData);
        mqMessage.setTime(new Date());
        // 协议一般为平台代码（字段暂无业务应用）
        mqMessage.setProtocol(device.getPfCode());
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());

        return mqMessage;
    }

    /**
     * 告警消息
     *
     * @param deviceId  设备ID
     * @param protocol  设备所属对接协议
     * @param facility  设备信息
     * @param eventType 告警类型
     * @param msgData   告警源消息体
     * @return mq message
     */
    public static MqMessage createAlarm(
            Long deviceId, String protocol, Facility facility, String eventType, String msgData
    ){
        return createAlarm(deviceId,protocol,facility,eventType,msgData,null);
    }

    /**
     * 告警消息
     *
     * @param deviceId  设备ID
     * @param protocol  设备所属对接协议
     * @param facility  设备信息
     * @param eventType 告警类型
     * @param msgData   告警源消息体
     * @param imageUrl  告警图片
     * @return the mq message
     */
    public static MqMessage createAlarm(
            Long deviceId, String protocol, Facility facility,
            String eventType, String msgData, String imageUrl
    ){
        MqMessage mqMessage = new MqMessage();
        mqMessage.setImageUrl(imageUrl);
        mqMessage.setDeviceId(deviceId);
        mqMessage.setEvent(MqMessage.EVENT_ALARM);
        mqMessage.setEventType(eventType);
        mqMessage.setFacility(facility);
        mqMessage.setMsgData(msgData);
        mqMessage.setTime(new Date());
        mqMessage.setProtocol(protocol);
        mqMessage.setUuid(SnowflakeIdWorker.getInstance().nextStringId());
        return mqMessage;
    }

    /**
     * 设备消息
     *
     * @param zaSysDevice the device
     * @return mq message
     */
    public static Facility createFacility(ZaSysDevice zaSysDevice){
        Facility facility = new Facility();
        facility.setWireless(zaSysDevice.getWireless() != null && zaSysDevice.getWireless().equalsIgnoreCase("Y"));
        facility.setCode(zaSysDevice.getCode());
        facility.setName(zaSysDevice.getName());
        facility.setModel(zaSysDevice.getModel());
        facility.setOnLine(zaSysDevice.getOnline().equalsIgnoreCase("1"));
        facility.setType(zaSysDevice.getType());
        facility.setNet(zaSysDevice.getNet());
        facility.setPfCode(zaSysDevice.getPfCode());
        return facility;
    }

    /**
     * The type Facility.
     */
    @Data
    public static class Facility{
        /**
         * 分类代码，青鸟是数字代码，其它平台可能传固定的设备分类代码
         */
        private String type;
        /**
         * 设备代码
         */
        private String code;

        /**
         * 通道号
         */
        private Integer chn;
        /**
         * 设备别名/安装位置
         */
        private String name;
        /**
         * 设备型号
         */
        private String model;
        /**
         * 设备传输设备（网关）
         */
        private String net;
        /**
         * 是否无线设备：1是0否
         */
        private boolean isWireless;
        /**
         * 是否在线：1是0否
         */
        private boolean isOnline;

        /**
         * The In online.
         */
        private boolean inOnline;

        /**
         * Set on line.
         *
         * @param isOnline the is online
         */
        public void setOnLine(Boolean isOnline){
            //兼容之前的拼写错误
            this.isOnline = isOnline;
            this.inOnline = isOnline;
        }

        /**
         * 信号量
         */
        private Integer rssi;
        /**
         * 电压
         */
        private Integer voltage;
        /**
         * 设备温度
         */
        private Integer temperature;

        /**
         * 平台ID
         */
        private String pfCode;
    }
}
