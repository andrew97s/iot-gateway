package com.zhian.gateway.third.jadebird.vo;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * @author luping
 * Description: 实时值（模拟量）
 * @version 1.0
 * @date 2024/11/28 14:33
 */
@Data
public class MeterV3 {
    //探测器节点编号，默认0
    private Integer nodeNo;
    //传感器类型，参考数据自动sensor
    private Integer sensorCode;
    //模拟量单位，参考数据字典meter_unit
    private Integer meterUnitCode;
    //模拟量单位描述
    private String meterUnitName;
    //模拟量值
    private BigDecimal meterValue;
    //更新时间戳（秒）
    private Integer time;

    public static void main(String[] args) {
        String file = FileUtil.readString("C:\\Users\\Administrator\\Desktop\\ft.txt", StandardCharsets.UTF_8);

        JSONObject dd = JSON.parseObject(file);
        JSONArray data = dd.getJSONArray("data");
        for (int i = 0; i < data.size(); i++) {
            JSONObject dt = data.getJSONObject(i);
            String code = dt.getString("code");
            String name = dt.getString("name");



            System.out.printf("青鸟设备类型:%s - %s \n", name, code);
        }
    }
}
