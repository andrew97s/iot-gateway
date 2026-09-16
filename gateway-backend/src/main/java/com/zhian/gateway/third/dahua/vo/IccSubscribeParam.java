package com.zhian.gateway.third.dahua.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ICC平台订阅参数
 * {
 *             "param": {
 *             "monitors": [
 *             {
 *                 "monitor": "http://10.35.111.10:8010/eventReceiveMsg/save",
 *                     "monitorType": "url",
 *                     "events": [
 *                 {
 *                     "category": "alarm",
 *                         "subscribeAll": 1,
 *                         "domainSubscribe": 2,
 *                         "authorities": [
 *                     {
 *                         "types": [
 *                         "57",
 *                                 "51",
 *                                 "61"
 *                                 ]
 *                     }
 *                         ]
 *                 },
 *                 {
 *                     "category": "business",
 *                         "subscribeAll": 1,
 *                         "domainSubscribe": 2,
 *                         "authorities": [
 *                     {
 *                         "types": [
 *                         "car.capture",
 *                                 "car.access"
 *                                 ]
 *                     }
 *                         ]
 *                 },
 *                 {
 *                     "category": "state",//事件类别，非报警事件无grade值
 *                         "subscribeAll": 1,//是否订阅所有（设备状态订阅所有过滤权限）
 *                         "domainSubscribe":2,
 *                         "authorities":[ //权限信息数组
 *                     {
 *                         "nodeCodes":[ //设备或通道，没有该字段就是订阅所有，空数组代表不订阅
 *                         "1000001",
 *                                 "1000002$1$0$1"
 *                                      ]
 *                     }
 *                         ]
 *                 },
 *                 {
 *                     "category": "perception",//事件类别，非报警事件无grade值
 *                         "subscribeAll": 1,//是否订阅所有（设备状态订阅所有过滤权限）
 *                         "domainSubscribe":2,
 *                         "authorities":[ //权限信息数组
 *                     {
 *                         "types":[ //设备或通道，没有该字段就是订阅所有，空数组代表不订阅
 *                         "pmms.perception.msg",
 *                                 "reportGPSInfo"
 *                                      ]
 *                     }
 *                         ]
 *                 }
 *                 ]
 *             }
 *         ],
 *             "subsystem": {
 *                 "subsystemType": 0,//固定值：0
 *                         "name": "10.35.111.10_8010",
 *                         "magic": "10.35.111.10_8010"
 *             }
 *         }
 *         }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IccSubscribeParam {
    private SubscribeParam param;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscribeParam{
        private SubscribeMonitor monitors[];
        private Subsystem subsystem;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscribeMonitor{
        private String monitor;
        private String monitorType;
       private SubscribeEvent[] events;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubscribeEvent {
        private String category;
        private Integer subscribeAll = 1;
        private Integer domainSubscribe = 1;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Subsystem{
        private Integer subsystemType = 0;
        private String name;
        private String magic;
    }
}