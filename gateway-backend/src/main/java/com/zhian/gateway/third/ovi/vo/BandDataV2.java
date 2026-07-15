package com.zhian.gateway.third.ovi.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * 手环推送数据
 *
 * @author tongwenjin
 * @since 2024 /8/12
 */
@Data
public class BandDataV2 {

    /**
     * 数据类型.
     */
    @JSONField(name = "DataType")
    private String dataType;

    /**
     * 业务数据.
     */
    @JSONField(name = "ResultData")
    private Data data;

    /**
     * 告警数据
     */
    @JSONField(name = "ResultData")
    private AlarmData alarmData;

    /**
     * 原始JSON数据
     */
    private String originalData;

    /**
     * The type Data.
     */
    @lombok.Data
    public static class Data {
        /**
         * The Imei.
         */
        private String imei;
        /**
         * 电量
         */
        private String battery;

        /**
         * 纬度.
         */
        private String latitude;
        /**
         * 经度.
         */
        private String longitude;
        /**
         * 型号.
         */
        private String model = "aqsh01";
        /**
         * The Status.
         */
        private String status;
        /**
         * 血氧.
         */
        private String bloodOxygen;
        /**
         * 血氧值测量时间.
         */
        private String bloodOxygenTime;
        /**
         * 心率.
         */
        private String heartRate;
        /**
         * 心率值测量时间.
         */
        private String hrTime;
        /**
         * 血压低值.
         */
        private String bloodPressureMin;
        /**
         * 血压高值.
         */
        private String bloodPressureMax;
        /**
         * 血压值测量时间.
         */
        private String bpTime;
        /**
         * 步数.
         */
        private String steps;
        /**
         * 深度睡眠时间.
         */
        private String deepSleep;
        /**
         * 浅度睡眠时间.
         */
        private String lighSleep;
        /**
         * 睡眠开始时间.
         */
        private String sleepTime;
        /**
         * 总睡眠时间.
         */
        private String totalSleep;
    }

    /**
     * The type Alarm data.
     */
    @lombok.Data
    public static class AlarmData {
        /**
         * The Imei.
         */
        private String SerialNumber;
        /**
         * ExceptionID
         */
        private String ExceptionID;
        /**
         * The Geo fence id.
         */
        private String GeoFenceID;
        /**
         * The Geo fence name.
         */
        private String GeoFenceName;
        /**
         * The Notification type.
         */
        private String NotificationType;
        /**
         * The Message.
         */
        private String Message;
        /**
         * The Created.
         */
        private String Created;
        /**
         * The Deleted.
         */
        private String Deleted;
        /**
         * The Clear date.
         */
        private String ClearDate;
        /**
         * The Clear by.
         */
        private String ClearBy;
        /**
         * The Note.
         */
        private String Note;
        /**
         * The Lat.
         */
        private String Lat;
        /**
         * The Lng.
         */
        private String Lng;
        /**
         * The Power.
         */
        private String Power;
        /**
         * The Address.
         */
        private String Address;
        /**
         * The Gsm.
         */
        private String GSM;
        /**
         * The Device utc time.
         */
        private String DeviceUTCTime;
        /**
         * The Serial no.
         */
        private String SerialNo;
    }


}
