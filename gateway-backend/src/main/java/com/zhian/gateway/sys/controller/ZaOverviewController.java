package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页概览聚合数据
 *
 * 一次请求返回首页所需的全部统计：设备在线情况、今日消息/告警/推送统计、
 * 消息类型分布、近24小时趋势、插件运行状态、最新告警。
 */
@Api("首页概览")
@RestController
@RequestMapping("/sys/overview")
public class ZaOverviewController extends BaseController {

    @Autowired
    private IZaSysDeviceService deviceService;

    @Autowired
    private IZaSysMessageService messageService;

    @Autowired
    private IZaSysPlatformService platformService;

    @ApiOperation("首页概览聚合数据")
    @GetMapping
    public AjaxResult overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // ---------- 设备统计（按平台 + 汇总） ----------
        List<Map<String, Object>> deviceByPlatform = deviceService.selectOnlineStats();
        long deviceTotal = 0, deviceOnline = 0;
        Map<String, Long> deviceCountByPf = new HashMap<>();
        for (Map<String, Object> row : deviceByPlatform) {
            long total = toLong(row.get("total"));
            long online = toLong(row.get("onlineCount"));
            deviceTotal += total;
            deviceOnline += online;
            Object pf = row.get("pfCode");
            if (pf != null) {
                deviceCountByPf.merge(pf.toString(), total, Long::sum);
            }
        }
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("total", deviceTotal);
        device.put("online", deviceOnline);
        device.put("offline", deviceTotal - deviceOnline);
        device.put("byPlatform", deviceByPlatform);
        data.put("device", device);

        // ---------- 今日消息 / 推送 / 告警 ----------
        Map<String, Object> today = messageService.selectTodaySendStats();
        data.put("today", today == null ? new HashMap<>() : today);
        data.put("typeDistribution", messageService.countTodayByType());
        data.put("trend", messageService.selectHourlyTrend());

        // ---------- 插件（平台）运行状态 ----------
        List<ZaSysPlatform> platforms = platformService.selectZaSysPlatformList(new ZaSysPlatform());
        List<Map<String, Object>> pluginList = new ArrayList<>();
        int runningCount = 0, enabledCount = 0;
        for (ZaSysPlatform pf : platforms) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pf.getId());
            item.put("code", pf.getCode());
            item.put("name", pf.getName());
            item.put("status", pf.getStatus());
            item.put("running", pf.getRunning());
            item.put("deviceCount", deviceCountByPf.getOrDefault(pf.getCode(), 0L));
            Map<String, Object> stats = ThirdApplicationRunner.getPlatformStats(pf.getCode());
            item.put("alive", Boolean.TRUE.equals(stats.get("alive")));
            item.put("protocol", stats.get("protocol"));
            item.put("msgCount", stats.get("msgCount"));
            item.put("errCount", stats.get("errCount"));
            item.put("lastStartTime", stats.get("lastStartTime"));
            pluginList.add(item);
            if ("1".equals(pf.getStatus())) {
                enabledCount++;
                if (Boolean.TRUE.equals(stats.get("alive"))) {
                    runningCount++;
                }
            }
        }
        Map<String, Object> plugin = new LinkedHashMap<>();
        plugin.put("total", platforms.size());
        plugin.put("enabled", enabledCount);
        plugin.put("running", runningCount);
        plugin.put("list", pluginList);
        data.put("plugin", plugin);

        // ---------- 消息累计（按平台） ----------
        data.put("messageByPlatform", messageService.countByPlatform());

        // ---------- 最新告警 ----------
        List<ZaSysMessage> alarms = messageService.selectLatestAlarms(8);
        data.put("latestAlarms", alarms);

        return success(data);
    }

    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
