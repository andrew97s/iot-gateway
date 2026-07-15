package com.zhian.gateway.third.ovi.vo;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

/**
 * 欧孚推送基础数据
 *
 * @author tongwenjin
 * @since 2024 /8/6
 */
@Data
@Slf4j
public class OviData {

    /**
     * 数据推送数据
     */
    private Date time;

    /**
     * 设备代码，对应IMEI
     */
    private String code;

    /**
     * 类型： 4： 步数.
     */
    private Integer type;

    private static final String DATE_PATTERN_1 = "yyyy/MM/dd HH:mm:ss";

    private static final String DATE_PATTERN_2 = "yyyy-MM-dd HH:mm:ss";

    /**
     * New instance ovi data.
     *
     * @param request the request
     * @return the ovi data
     */
    public static OviData newInstance(Map<String, String> request) {
        OviData data = new OviData();

        try {
            // 事件类型
            data.setType(Integer.parseInt(request.get("type")));

            // 推送时间
            String time = request.containsKey("BTUtcTime") ? request.get("BTUtcTime") : request.get("timeStr");
            data.setTime(
                    DateUtil.parse(
                                    time, DATE_PATTERN_1, DATE_PATTERN_2
                            )
                            // 时区原因， 此处时间需要+8小时
                            .offset(DateField.HOUR_OF_DAY, +8)
            );

            // 设备编码
            data.setCode(request.get("IMEI"));
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("初始化欧孚推送数据失败,数据格式异常!");
            return null;
        }

        return data;
    }
}
