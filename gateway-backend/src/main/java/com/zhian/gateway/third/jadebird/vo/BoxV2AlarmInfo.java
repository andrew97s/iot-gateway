package com.zhian.gateway.third.jadebird.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 云盒V2信息VO
 *
 * @author tongwenjin
 * @since 2024-9-25
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class BoxV2AlarmInfo extends BoxV2Info {
    //设备机器码
    private String machineCode;
    // 报警图片base64(此处告警图片较大)
    private String alarmPicData;
    //报警图片名称
    private String alarmPicName;
    //摄像头对应拉流地址
    private String cameraUrl;
    //图片高度
    private String imageHeight;
    //图片宽度
    private String imageWidth;
    private String taskId;
    //时间戳
    private String timestamp;
    //摄像头唯一id
    private String cameraId;
    //摄像头别名
    private String cameraName;
    // 报警原图base64
    private String srcPicData;
    //报警原图名称
    private String srcPicName;
    //进入人数
    private String in;
    //出去人数
    private String out;

    // 产生的告警集合
    private List<AlarmInfo> alarmArray;

    @Data
    public static class AlarmInfo {
        //算法ID，定义的算法ID 参照表如下
        // 0	未戴安全帽, 2	未穿反光衣, 4	行人闯入, 7	烟火, 9	抽烟, 10	打电话, 22	红色安全帽, 23	反光衣, 24	玩手机,
        // 25	摩托车, 26	蓝色安全帽, 27	白色安全帽, 28	黄色安全帽, 29	汽车检测, 30	大巴, 31	汽车SUV, 32	货车,
        // 33	自行车, 34	卡车, 36	搅拌车, 37	自卸卡车, 38	挖掘机, 39	上半身, 40	人员离岗, 41	人员聚众, 42	区域人数,
        // 43	出入人数, 44	超员检测, 45	少员检测, 46	人形检测, 47	人员徘徊, 48	翻墙检测, 49	区域入侵, 50	通道占用,
        // 51	车辆离开, 52	人脸识别
        private String classid;
        private String en_name;
        private String name;
        //框的高度
        private String height;
        //框的宽度
        private String width;
        //框的 x坐标
        private String x;
        //框的y坐标
        private String y;
        //人数
        private String count;
        //如何报警是人脸则是人脸的唯一id
        private String userid;
        //人脸分组id
        private String groupid;
    }
}
