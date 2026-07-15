package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysDevice;
import com.zhian.gateway.sys.service.IZaSysDeviceService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

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

    /**
     * 不分页查询接入设备列表
     */
    @ApiOperation("不分页查询接入设备列表")
    @GetMapping("/select")
    public AjaxResult select(ZaSysDevice zaSysDevice)
    {
        List<ZaSysDevice> list = deviceService.selectZaSysDeviceList(zaSysDevice);
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
        return getDataTable(list);
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
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
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
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(deviceService.selectZaSysDeviceById(id));
    }

    /**
     * 新增接入设备
     */
    @ApiOperation("新增接入设备")
    @PreAuthorize("@ss.hasPermi('sys:device:add')")
    @Log(title = "接入设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaSysDevice zaSysDevice)
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
    public AjaxResult edit(@RequestBody ZaSysDevice zaSysDevice)
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
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(deviceService.deleteZaSysDeviceByIds(ids));
    }


    /**
     * 同步设备信息，向外部应用推送
     */
    @ApiOperation("同步设备信息，向外部应用推送")
    @GetMapping(value = "/sync")
    public AjaxResult sync(@PathVariable("id") Long id)
    {
        deviceService.pushDevice();
        return success();
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public AjaxResult purge()
    {
        deviceService.purge();
        return toAjax(true);
    }

    /**
     * 按平台统计设备在线/总数
     */
    @ApiOperation("按平台统计设备在线情况")
    @GetMapping("/online-stats")
    public AjaxResult onlineStats()
    {
        return success(deviceService.selectOnlineStats());
    }

    /**
     * 更新设备在线状态，并推送事件给下游
     */
    @ApiOperation("更新设备在线状态")
    @PreAuthorize("@ss.hasPermi('sys:device:edit')")
    @PutMapping("/online")
    public AjaxResult updateOnline(@RequestBody java.util.Map<String, Object> params)
    {
        String code   = (String)  params.get("code");
        String pfCode = (String)  params.get("pfCode");
        Boolean online = Boolean.TRUE.equals(params.get("online"));
        deviceService.updateDeviceOnline(code, pfCode, online);
        return success();
    }
}
