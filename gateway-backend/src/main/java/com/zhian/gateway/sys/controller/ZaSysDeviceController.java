package com.zhian.gateway.sys.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysUpstream;
import com.zhian.gateway.sys.mapper.ZaAlarmTypeMapper;
import com.zhian.gateway.sys.mapper.ZaSysMessageMapper;
import com.zhian.gateway.sys.mapper.ZaSysUpstreamMapper;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接入设备Controller
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
@Api("接入设备")
@RestController
@RequestMapping("/sys/device")
public class ZaSysDeviceController extends BaseController
{
    @Autowired
    private IZaSysDeviceService deviceService;
    @Autowired
    private ZaSysUpstreamMapper upstreamMapper;
    @Autowired
    private ZaSysMessageMapper messageMapper;
    @Autowired
    private ZaAlarmTypeMapper alarmTypeMapper;

    /**
     * 不分页查询接入设备列表
     */
    @ApiOperation("不分页查询接入设备列表")
    @GetMapping("/select")
    public R select(ZaSysDevice zaSysDevice)
    {
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(zaSysDevice);
        enrichSync(list);
        return success(list);
    }

    /**
     * 分页查询接入设备列表
     */
    @ApiOperation("分页查询接入设备列表")
    @PreAuthorize("@ss.hasPermi('sys:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysDevice zaSysDevice)
    {
        startPage();
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(zaSysDevice);
        enrichSync(list);
        return getDataTable(list);
    }

    private void enrichSync(List<ZaSysDevice> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        int total = 0;
        try {
            Long cnt = upstreamMapper.selectCount(Wrappers.lambdaQuery(ZaSysUpstream.class)
                    .eq(ZaSysUpstream::getStatus, ZaSysUpstream.STATUS_ENABLED));
            total = cnt == null ? 0 : cnt.intValue();
        } catch (Exception ignored) {
        }
        for (ZaSysDevice d : list) {
            d.setSyncTotal(total);
            if (total <= 0) {
                d.setSyncSuccess(0);
                d.setSyncLabel("未配置上级");
            } else if ("1".equals(d.getOnline())) {
                d.setSyncSuccess(total);
                d.setSyncLabel(total + "/" + total + " 已同步");
            } else {
                d.setSyncSuccess(0);
                d.setSyncLabel("未同步");
            }
        }
    }

    /**
     * 导出接入设备列表
     */
    @ApiOperation("导出接入设备列表")
    @PreAuthorize("@ss.hasPermi('sys:device:export')")
    @Log(title = "接入设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysDevice zaSysDevice)
    {
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(zaSysDevice);
        ExcelUtil<ZaSysDevice> util = new ExcelUtil<ZaSysDevice>(ZaSysDevice.class);
        util.exportExcel(response, list, "接入设备数据");
    }

    @Log(title = "建筑物管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('sys:device:import')")
    @PostMapping("/importData")
    public R importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<ZaSysDevice> util = new ExcelUtil<ZaSysDevice>(ZaSysDevice.class);
        List<ZaSysDevice> dataList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = deviceService.importData(dataList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws Exception
    {
        ExcelUtil<ZaSysDevice> util = new ExcelUtil<ZaSysDevice>(ZaSysDevice.class);
        util.importTemplateExcel(response, "设备导入模板");
    }

    /**
     * 获取接入设备详细信息
     */
    @ApiOperation("获取接入设备详细信息")
    @ApiImplicitParam(name = "id", value = "接入设备主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:device:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(deviceService.selectZaSysDeviceById(id));
    }

    /**
     * 获取设备最近一次上报的各项监测值。
     */
    @ApiOperation("设备实时监测数据")
    @PreAuthorize("@ss.hasPermi('sys:device:list')")
    @GetMapping("/{id}/telemetry")
    public R telemetry(@PathVariable("id") Long id)
    {
        ZaSysDevice device = deviceService.selectZaSysDeviceById(id);
        if (device == null) {
            return error("设备不存在");
        }

        List<ZaSysMessage> messages = messageMapper.selectLatestTelemetry(id, 50);
        Map<String, JSONObject> latest = new LinkedHashMap<>();
        long updatedAt = 0L;
        for (ZaSysMessage message : messages) {
            List<JSONObject> unifiedMessages = parseUnifiedMessages(message.getUnifiedContent());
            for (int messageIndex = unifiedMessages.size() - 1; messageIndex >= 0; messageIndex--) {
                JSONObject unified = unifiedMessages.get(messageIndex);
                if (!"telemetry".equalsIgnoreCase(unified.getString("type"))) {
                    continue;
                }
                JSONObject payload = unified.getJSONObject("payload");
                JSONArray telemetries = payload == null ? null : payload.getJSONArray("telemetries");
                if (telemetries == null) {
                    continue;
                }
                for (int i = 0; i < telemetries.size(); i++) {
                    JSONObject item = telemetries.getJSONObject(i);
                    String code = item == null ? null : item.getString("code");
                    if (code == null || code.trim().isEmpty()) {
                        continue;
                    }
                    int channel = item.getIntValue("channel", 1);
                    item.put("channel", channel);
                    long timestamp = item.getLongValue("timestamp");
                    if (timestamp <= 0L) {
                        timestamp = unified.getLongValue("timestamp");
                    }
                    if (timestamp <= 0L) {
                        Date createTime = message.getCreateTime();
                        timestamp = createTime == null ? 0L : createTime.getTime();
                    }
                    if (timestamp > 0L) {
                        item.put("timestamp", timestamp);
                    }
                    String key = code + ":" + channel;
                    if (!latest.containsKey(key)) {
                        latest.put(key, item);
                        updatedAt = Math.max(updatedAt, timestamp);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", device.getId());
        result.put("deviceCode", device.getCode());
        result.put("updatedAt", updatedAt);
        result.put("items", new ArrayList<>(latest.values()));
        return success(result);
    }

    /**
     * 获取设备最近的告警记录。
     */
    @ApiOperation("设备告警记录")
    @PreAuthorize("@ss.hasPermi('sys:device:list')")
    @GetMapping("/{id}/alarms")
    public R alarms(@PathVariable("id") Long id)
    {
        ZaSysDevice device = deviceService.selectZaSysDeviceById(id);
        if (device == null) {
            return error("设备不存在");
        }

        List<Map<String, Object>> records = new ArrayList<>();
        List<ZaSysMessage> messages = messageMapper.selectLatestAlarms(id, 30);
        Map<String, Integer> alarmCategories = new HashMap<>();
        for (ZaAlarmType alarmType : alarmTypeMapper.selectList(null)) {
            if (alarmType.getCode() != null) {
                alarmCategories.put(alarmType.getCode(), alarmType.getType());
            }
        }
        for (ZaSysMessage message : messages) {
            List<JSONObject> unifiedMessages = parseUnifiedMessages(message.getUnifiedContent());
            for (int i = unifiedMessages.size() - 1; i >= 0 && records.size() < 20; i--) {
                JSONObject unified = unifiedMessages.get(i);
                if (!"alarm".equalsIgnoreCase(unified.getString("type"))) {
                    continue;
                }
                JSONObject payload = unified.getJSONObject("payload");
                if (payload == null) {
                    continue;
                }
                long timestamp = payload.getLongValue("timestamp");
                if (timestamp <= 0L) {
                    timestamp = unified.getLongValue("timestamp");
                }
                if (timestamp <= 0L && message.getCreateTime() != null) {
                    timestamp = message.getCreateTime().getTime();
                }

                Map<String, Object> record = new LinkedHashMap<>();
                record.put("messageId", unified.getString("id"));
                String alarmCode = payload.getString("code");
                record.put("code", alarmCode);
                record.put("name", payload.getString("name"));
                record.put("category", payload.getString("type"));
                record.put("state", payload.getString("state"));
                record.put("desc", payload.getString("desc"));
                record.put("picUrl", payload.getString("picUrl"));
                record.put("timestamp", timestamp);
                record.put("rawContent", message.getContent());
                records.add(record);
            }
            if (records.size() >= 20) {
                break;
            }
        }
        return success(records);
    }

    private List<JSONObject> parseUnifiedMessages(String content)
    {
        List<JSONObject> messages = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return messages;
        }
        try {
            Object parsed = JSON.parse(content);
            if (parsed instanceof JSONObject) {
                messages.add((JSONObject) parsed);
            } else if (parsed instanceof JSONArray) {
                JSONArray array = (JSONArray) parsed;
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    if (item != null) {
                        messages.add(item);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return messages;
    }

    /**
     * 新增接入设备
     */
    @ApiOperation("新增接入设备")
    @PreAuthorize("@ss.hasPermi('sys:device:add')")
    @Log(title = "接入设备", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysDevice zaSysDevice)
    {
        zaSysDevice.setCreateBy(getUsername());
        return toAjax(deviceService.insertZaSysDevice(zaSysDevice));
    }

    /**
     * 修改接入设备
     */
    @ApiOperation("修改接入设备")
    @PreAuthorize("@ss.hasPermi('sys:device:edit')")
    @Log(title = "接入设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysDevice zaSysDevice)
    {
        zaSysDevice.setUpdateBy(getUsername());
        return toAjax(deviceService.updateZaSysDevice(zaSysDevice));
    }

    /**
     * 删除接入设备
     */
    @ApiOperation("删除接入设备")
    @ApiImplicitParam(name = "ids", value = "接入设备主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:device:remove')")
    @Log(title = "接入设备", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(deviceService.deleteZaSysDeviceByIds(ids));
    }


    /**
     * 同步设备信息，向外部应用推送
     */
    @ApiOperation("同步设备信息，向外部应用推送")
    @GetMapping(value = "/sync")
    public R sync()
    {
        deviceService.pushDevice();
        return success();
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public R purge()
    {
        deviceService.purge();
        return toAjax(true);
    }

    /**
     * 按平台统计设备在线/总数
     */
    @ApiOperation("按平台统计设备在线情况")
    @GetMapping("/online-stats")
    public R onlineStats()
    {
        return success(deviceService.selectOnlineStats());
    }

    /**
     * 更新设备在线状态，并推送事件给下游
     */
    @ApiOperation("更新设备在线状态")
    @PreAuthorize("@ss.hasPermi('sys:device:edit')")
    @PutMapping("/online")
    public R updateOnline(@RequestBody java.util.Map<String, Object> params)
    {
        String code   = (String)  params.get("code");
        String pfCode = (String)  params.get("pfCode");
        Boolean online = Boolean.TRUE.equals(params.get("online"));
        deviceService.updateDeviceOnline(code, pfCode, online);
        return success();
    }
}
