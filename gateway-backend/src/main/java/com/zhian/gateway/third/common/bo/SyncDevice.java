package com.zhian.gateway.third.common.bo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备同步B0
 *
 * @author tongwenjin
 * @since 2024 /8/1
 */
@Data
@Builder
public class SyncDevice {

    Long id;

    /**
     * The Code.
     */
    String code;

    /**
     * The Name.
     */
    String name;

    /**
     * The Net.
     */
    String net;

    /**
     * The Model.
     */
    String model;

    /**
     * The Type code.
     */
    String typeCode;

    /**
     * The Pf code.
     */
    String pfCode;

    /**
     * The Wireless.
     */
    String wireless;

    /**
     * The Ip.
     */
    String ip;

    /**
     * 是否在线（1在线，0离线）
     */
    String online;

    /**
     * The Remark.
     */
    String remark;

    public static void main(String[] args) {
        String json = FileUtil.readString("C:\\Users\\Administrator\\Desktop\\aa.txt", StandardCharsets.UTF_8);
        JSONObject obj = JSON.parseObject(json);
        JSONArray data = obj.getJSONArray("data");
        System.out.println("code - name - type - cancel - recovery : ");

        String sql =  "insert into za_monitor_type(id, code, name, unit, aliases, create_time)\n" +
                "values";
        List<String> codes = Arrays.asList("1", "2", "6");
        for (int i = 0; i < data.size(); i++) {
            JSONObject jb = data.getJSONObject(i);

            sql +=
                     "(10000" + jb.getString("code") + ","
                    + "'" + jb.getString("code") + "',"
                    + "'" + jb.getString("name") + "',"
                    + "'" + jb.getString("unit") + "',"
                    + "'[{\"pfCode\":\"jb\",\"alias\":\""+jb.getString("code")+"\"}]',"
                    +   "now()),";

        }

        System.out.println(sql);
    }
}
