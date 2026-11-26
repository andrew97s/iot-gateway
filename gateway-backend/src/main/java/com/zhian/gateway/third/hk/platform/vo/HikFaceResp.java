package com.zhian.gateway.third.hk.platform.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 海康人脸查询结果
 *
 * @author tongwenjin
 * @since 2024 -12-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HikFaceResp extends HikResult {

    /**
     * The Data.
     */
    private Result data;

    /**
     * The type Result.
     */
    @Data
    public static class Result {
        /**
         * The Total.
         */
        private String total;
        /**
         * The Page no.
         */
        private String pageNo;
        /**
         * The Page size.
         */
        private String pageSize;
        /**
         * The List.
         */
        private List<PersonResult> list;
    }


    /**
     * The type Person result.
     */
    @Data
    public static class PersonResult {
        /**
         * The Camera index code.
         */
        private String cameraIndexCode;
        /**
         * The Capture time.
         */
        private String captureTime;
        /**
         * The Sex.
         */
        private String sex;
        /**
         * The Age group.
         */
        private String ageGroup;
        /**
         * The With glass.
         */
        private String withGlass;
        /**
         * The Similarity.
         */
        private String similarity;
        /**
         * The Bkg pic url.
         */
        private String bkgPicUrl;
        /**
         * The Face pic url.
         */
        private String facePicUrl;
        /**
         * The Rect.
         */
        private String rect;
    }
}
