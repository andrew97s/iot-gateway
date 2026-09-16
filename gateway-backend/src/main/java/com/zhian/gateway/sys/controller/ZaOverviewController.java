package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.plugin.PluginInstanceView;
import com.zhian.gateway.plugin.PluginManager;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private PluginManager pluginManager;

    @ApiOperation("首页概览聚合数据")
    @GetMapping
    public R overview() {
        Map<String, Object> data = new LinkedHashMap<>();

        // ---------- 设备统计（按平台 + 汇总） ----------
        List<Map<String, Object>> deviceByPlatform = deviceService.selectOnlineStats();
        long deviceTotal = 0, deviceOnline = 0;
        for (Map<String, Object> row : deviceByPlatform) {
            long total = toLong(row.get("total"));
            long online = toLong(row.get("onlineCount"));
            deviceTotal += total;
            deviceOnline += online;
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
        List<PluginInstanceView> pluginList = pluginManager.listInstances(null, null, null);
        long runningCount = pluginList.stream().filter(p -> "running".equals(p.getState())).count();
        long stoppedCount = pluginList.stream().filter(p -> "stopped".equals(p.getState())).count();
        long installedCount = pluginList.stream().filter(p -> "installed".equals(p.getState())).count();
        long abnormalCount = pluginList.stream().filter(p -> "abnormal".equals(p.getState())).count();
        Map<String, Object> plugin = new LinkedHashMap<>();
        plugin.put("total", pluginList.size());
        plugin.put("enabled", runningCount + abnormalCount);
        plugin.put("running", runningCount);
        plugin.put("stopped", stoppedCount);
        plugin.put("installed", installedCount);
        plugin.put("abnormal", abnormalCount);
        plugin.put("list", pluginList);
        data.put("plugin", plugin);

        // ---------- 消息统计（按来源平台，优先今日） ----------
        List<Map<String, Object>> todayByPf = messageService.countTodayByPlatform();
        data.put("messageByPlatform", todayByPf != null && !todayByPf.isEmpty()
                ? todayByPf
                : messageService.countByPlatform());

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
