package com.zhian.gateway.third.video.roc.event;

import lombok.Data;

import java.util.List;

/**
 * ROC api event
 *
 * @author tongwenjin
 * @since 2025-2-20
 */

@Data
public class RocEvent {

    private String SerialNum;

    private String StartTime;

    private String EventType;

    private String OverallImgInfo;

    private FaceCompareInfo FaceCompareInfo;

    @Data
    public static class FaceCompareInfo {
        private List<Snap> Snap;
    }

    @Data
    public static class Snap {
        private FaceInImg FaceInImg;
        private Match Match;
    }

    @Data
    public static class FaceInImg {
        private String ImgName;
    }

    @Data
    public static class Match {
        private String Id;

        private String Name;

        private String GroupName;

        private String GroupType;

        private String Similarity;

        private String RegPic;

        private String Feature;
    }
}
