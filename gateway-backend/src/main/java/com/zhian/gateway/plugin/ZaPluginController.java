package com.zhian.gateway.plugin;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 接入插件管理 API（对齐设计原型 /api/plugins）
 */
@Api("接入插件管理")
@RestController
@RequestMapping("/sys/plugin")
public class ZaPluginController extends BaseController {

    @Autowired
    private PluginManager pluginManager;
    @Autowired
    private PluginPackageService packageService;
    @Autowired
    private PluginCatalog catalog;
    @Autowired
    private IZaSysPlatformService platformService;
    @Autowired
    private IZaPlatformLogService platformLogService;
    @Autowired
    private IZaSysMessageService messageService;

    @ApiOperation("插件运行摘要")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        return success(pluginManager.summary());
    }

    @ApiOperation("插件类型目录")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/types")
    public AjaxResult types() {
        return success(pluginManager.listTypes());
    }

    @ApiOperation("插件类型详情")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/types/{pluginId}")
    public AjaxResult typeDetail(@PathVariable String pluginId) {
        return success(pluginManager.getType(pluginId));
    }

    @ApiOperation("上传安装插件包（zip：plugin.yaml + config-schema.json）")
    @PreAuthorize("@ss.hasPermi('sys:platform:add')")
    @Log(title = "安装插件包", businessType = BusinessType.INSERT)
    @PostMapping("/packages")
    public AjaxResult installPackage(@RequestParam("file") MultipartFile file) {
        return success(packageService.installPackage(file));
    }

    @ApiOperation("插件实例列表")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/instances")
    public AjaxResult instances(String keyword, String state, String protocol) {
        return success(pluginManager.listInstances(keyword, state, protocol));
    }

    @ApiOperation("插件实例详情")
    @PreAuthorize("@ss.hasPermi('sys:platform:query')")
    @GetMapping("/instances/{instanceId}")
    public AjaxResult instanceDetail(@PathVariable String instanceId) {
        return success(pluginManager.getInstance(instanceId));
    }

    @ApiOperation("从类型创建插件实例")
    @PreAuthorize("@ss.hasPermi('sys:platform:add')")
    @Log(title = "安装插件实例", businessType = BusinessType.INSERT)
    @PostMapping("/instances")
    public AjaxResult createInstance(@RequestBody Map<String, Object> body) {
        String pluginId = str(body.get("pluginId"));
        String instanceId = str(body.get("instanceId"));
        String name = str(body.get("name"));
        String ip = str(body.get("ip"));
        Integer port = body.get("port") == null || "".equals(body.get("port"))
                ? null : Integer.valueOf(String.valueOf(body.get("port")));
        return success(pluginManager.install(pluginId, instanceId, name, ip, port));
    }

    @ApiOperation("更新实例配置（热生效）")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "插件配置", businessType = BusinessType.UPDATE)
    @PutMapping("/instances/{instanceId}/config")
    public AjaxResult updateConfig(@PathVariable String instanceId, @RequestBody Map<String, Object> body) {
        return success(pluginManager.updateConfig(instanceId, body));
    }

    @ApiOperation("启动插件实例")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "启动插件", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{instanceId}/start")
    public AjaxResult start(@PathVariable String instanceId) {
        return success(pluginManager.start(instanceId));
    }

    @ApiOperation("停止插件实例")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "停止插件", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{instanceId}/stop")
    public AjaxResult stop(@PathVariable String instanceId) {
        return success(pluginManager.stop(instanceId));
    }

    @ApiOperation("重启插件实例")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "重启插件", businessType = BusinessType.UPDATE)
    @PostMapping("/instances/{instanceId}/restart")
    public AjaxResult restart(@PathVariable String instanceId) {
        return success(pluginManager.restart(instanceId));
    }

    @ApiOperation("卸载插件实例")
    @PreAuthorize("@ss.hasPermi('sys:platform:remove')")
    @Log(title = "卸载插件", businessType = BusinessType.DELETE)
    @DeleteMapping("/instances/{instanceId}")
    public AjaxResult uninstall(@PathVariable String instanceId) {
        pluginManager.uninstall(instanceId);
        return success();
    }

    @ApiOperation("测试连接")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @PostMapping("/instances/{instanceId}/test")
    public AjaxResult test(@PathVariable String instanceId) {
        return success(pluginManager.testConnection(instanceId));
    }

    @ApiOperation("实例消息统计")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/instances/{instanceId}/stats")
    public AjaxResult stats(@PathVariable String instanceId) {
        pluginManager.getInstance(instanceId); // 校验存在
        Map<String, Object> data = new LinkedHashMap<>();
        long total = 0L, failed = 0L, todayTotal = 0L, todayFailed = 0L;
        List<Map<String, Object>> all = messageService.countByPlatform();
        if (all != null) {
            for (Map<String, Object> row : all) {
                if (instanceId.equals(String.valueOf(row.get("pfCode")))) {
                    total = toLong(row.get("total"));
                    failed = toLong(row.get("failedCount"));
                    break;
                }
            }
        }
        List<Map<String, Object>> today = messageService.countTodayByPlatform();
        if (today != null) {
            for (Map<String, Object> row : today) {
                if (instanceId.equals(String.valueOf(row.get("pfCode")))) {
                    todayTotal = toLong(row.get("total"));
                    todayFailed = toLong(row.get("failedCount"));
                    break;
                }
            }
        }
        data.put("total", total);
        data.put("failedCount", failed);
        data.put("todayTotal", todayTotal);
        data.put("todayFailed", todayFailed);
        data.put("typeDist", messageService.countTodayByTypeAndPlatform(instanceId));
        data.put("hourlyTrend", messageService.selectHourlyTrendByPlatform(instanceId));
        data.put("runtime", com.zhian.gateway.third.ThirdApplicationRunner.getPlatformStats(
                PluginManager.resolvePluginId(platformService.selectZaSysPlatformByCode(instanceId))));
        data.put("restartCount", pluginManager.getRestartCount(instanceId));
        return success(data);
    }

    @ApiOperation("实例运行日志")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/instances/{instanceId}/logs")
    public TableDataInfo logs(@PathVariable String instanceId) {
        ZaSysPlatform p = platformService.selectZaSysPlatformByCode(instanceId);
        if (p == null) {
            return getDataTable(Collections.emptyList());
        }
        startPage();
        List<ZaPlatformLog> list = platformLogService.selectByPlatformId(p.getId());
        return getDataTable(list);
    }

    @ApiOperation("清空实例运行日志")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "清空插件日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/instances/{instanceId}/logs")
    public AjaxResult clearLogs(@PathVariable String instanceId) {
        ZaSysPlatform p = platformService.selectZaSysPlatformByCode(instanceId);
        if (p == null) {
            return error("实例不存在");
        }
        return toAjax(platformLogService.clearByPlatformId(p.getId()));
    }

    @ApiOperation("刷新插件目录")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @PostMapping("/catalog/refresh")
    public AjaxResult refreshCatalog() {
        catalog.refresh();
        return success(catalog.listTypes());
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }
}
