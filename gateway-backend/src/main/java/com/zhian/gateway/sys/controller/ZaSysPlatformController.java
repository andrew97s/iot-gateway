package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.consts.DictValue;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.zhian.gateway.sys.domain.ZaPlatformLog;
import com.zhian.gateway.sys.domain.ZaSysPlatform;
import com.zhian.gateway.sys.service.IZaPlatformLogService;
import com.zhian.gateway.sys.service.IZaSysPlatformService;
import com.zhian.gateway.third.ThirdApplicationRunner;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.zhian.gateway.common.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

/**
 * 平台信息Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("平台信息")
@RestController
@RequestMapping("/sys/platform")
public class ZaSysPlatformController extends BaseController
{
    @Autowired
    private IZaSysPlatformService platformService;
    @Autowired
    private IZaPlatformLogService zaPlatformLogService;

    /**
     * 不分页查询平台信息列表
     */
    @ApiOperation("不分页查询平台信息列表")
    @GetMapping("/select")
    public AjaxResult select(ZaSysPlatform platform)
    {
        List<ZaSysPlatform> list = platformService.selectZaSysPlatformList(platform);
        return success(list);
    }

    /**
     * 分页查询平台信息列表
     */
    @ApiOperation("分页查询平台信息列表")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysPlatform platform)
    {
        startPage();
        List<ZaSysPlatform> list = platformService.selectZaSysPlatformList(platform);
        return getDataTable(list);
    }

    /**
     * 导出平台信息列表
     */
    @ApiOperation("导出平台信息列表")
    @PreAuthorize("@ss.hasPermi('sys:platform:export')")
    @Log(title = "平台信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysPlatform platform)
    {
        List<ZaSysPlatform> list = platformService.selectZaSysPlatformList(platform);
        ExcelUtil<ZaSysPlatform> util = new ExcelUtil<ZaSysPlatform>(ZaSysPlatform.class);
        util.exportExcel(response, list, "平台信息数据");
    }

    /**
     * 获取平台信息详细信息
     */
    @ApiOperation("获取平台信息详细信息")
    @ApiImplicitParam(name = "id", value = "平台信息主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:platform:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(platformService.selectZaSysPlatformById(id));
    }

    /**
     * 新增平台信息
     */
    @ApiOperation("新增平台信息")
    @PreAuthorize("@ss.hasPermi('sys:platform:add')")
    @Log(title = "平台信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaSysPlatform platform)
    {
        platform.setCreateBy(getUsername());
        return toAjax(platformService.insertZaSysPlatform(platform));
    }

    /**
     * 修改平台信息
     */
    @ApiOperation("修改平台信息")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "平台信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ZaSysPlatform platform)
    {
        platform.setUpdateBy(getUsername());
        AjaxResult result = toAjax(platformService.updateZaSysPlatform(platform));
        // 启用状态下 - 重启
        if(platform.getStatus().equalsIgnoreCase(DictValue.STATUS_ENABLE)) {
            platformService.triggerPlatform(platform.getCode(), false);
            platformService.triggerPlatform(platform.getCode(), true);
        }
        // 关闭状态下 - 关闭
        else {
            platformService.triggerPlatform(platform.getCode(), false);
        }
        if (platform.getId() != null && StringUtils.isNotEmpty(platform.getCode())) {
            String pname = StringUtils.isNotEmpty(platform.getName())
                    ? platform.getName() : platform.getCode();
            String detail = buildPlatformConfigurationSnapshot(platform);
            zaPlatformLogService.recordByPlatformId(platform.getId(), ZaPlatformLog.TYPE_CONFIG,
                    pname + " 配置已更新并重新加载", detail);
        }
        return result;
    }

    /**
     * 删除平台信息
     */
    @ApiOperation("删除平台信息")
    @ApiImplicitParam(name = "ids", value = "平台信息主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:platform:remove')")
    @Log(title = "平台信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(platformService.deleteZaSysPlatformByIds(ids));
    }

    /**
     * 启动平台对接
     */
    @ApiOperation("启动平台对接")
    @ApiImplicitParam(name = "code", value = "平台代码", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:platform:add')")
    @GetMapping(value = "/start/{code}")
    public AjaxResult start(@PathVariable("code") String code)
    {
        return success(platformService.triggerPlatform(code, true));
    }

    /**
     * 停止平台对接
     */
    @ApiOperation("停止平台对接")
    @ApiImplicitParam(name = "code", value = "平台代码", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:platform:add')")
    @GetMapping(value = "/stop/{code}")
    public AjaxResult stop(@PathVariable("code") String code)
    {
        return success(platformService.triggerPlatform(code, false));
    }

    /**
     * 获取指定平台的运行时统计信息（注册状态、消息数、错误数、最近启动时间等）
     */
    @ApiOperation("获取平台运行时统计")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping(value = "/stats/{code}")
    public AjaxResult stats(@PathVariable("code") String code)
    {
        return success(ThirdApplicationRunner.getPlatformStats(code));
    }

    /**
     * 获取所有平台的运行时统计信息列表
     */
    @ApiOperation("获取所有平台运行时统计")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping(value = "/stats")
    public AjaxResult statsAll()
    {
        return success(ThirdApplicationRunner.getAllPlatformStats());
    }

    /**
     * 分页查询插件运行日志（za_platform_log）
     */
    @ApiOperation("分页查询平台运行日志")
    @PreAuthorize("@ss.hasPermi('sys:platform:list')")
    @GetMapping(value = "/logs/{code}")
    public TableDataInfo logs(@PathVariable("code") String code)
    {
        ZaSysPlatform p = platformService.selectZaSysPlatformByCode(code);
        if (p == null) {
            return getDataTable(Collections.emptyList());
        }
        startPage();
        List<ZaPlatformLog> list = zaPlatformLogService.selectByPlatformId(p.getId());
        return getDataTable(list);
    }

    /**
     * 清空指定平台的运行日志
     */
    @ApiOperation("清空平台运行日志")
    @PreAuthorize("@ss.hasPermi('sys:platform:edit')")
    @Log(title = "平台运行日志", businessType = BusinessType.DELETE)
    @DeleteMapping(value = "/logs/{code}")
    public AjaxResult clearLogs(@PathVariable("code") String code)
    {
        ZaSysPlatform p = platformService.selectZaSysPlatformByCode(code);
        if (p == null) {
            return error("平台不存在");
        }
        return toAjax(zaPlatformLogService.clearByPlatformId(p.getId()));
    }

    /**
     * 生成保存后的完整配置快照（JSON），写入运行日志详情
     */
    private static String buildPlatformConfigurationSnapshot(ZaSysPlatform platform)
    {
        JSONObject root = new JSONObject();
        root.put("name", platform.getName());
        root.put("code", platform.getCode());
        root.put("ip", platform.getIp());
        root.put("port", platform.getPort());
        root.put("apis", platform.getApis());
        root.put("status", platform.getStatus());
        root.put("running", platform.getRunning());
        root.put("remark", platform.getRemark());
        if (StringUtils.isNotEmpty(platform.getConfig())) {
            try {
                root.put("configObject", JSON.parseObject(platform.getConfig()));
            } catch (Exception e) {
                root.put("configRaw", platform.getConfig());
            }
        } else {
            root.put("configObject", new JSONObject());
        }
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }
}
