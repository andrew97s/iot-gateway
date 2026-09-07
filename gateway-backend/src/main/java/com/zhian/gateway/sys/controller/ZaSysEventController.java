package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysEvent;
import com.zhian.gateway.sys.service.IZaSysEventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 告警事件Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("告警事件")
@RestController
@RequestMapping("/sys/event")
public class ZaSysEventController extends BaseController
{
    @Autowired
    private IZaSysEventService zaSysEventService;

    /**
     * 不分页查询告警事件列表
     */
    @ApiOperation("不分页查询告警事件列表")
    @GetMapping("/select")
    public R select(ZaSysEvent zaSysEvent)
    {
        List<ZaSysEvent> list = zaSysEventService.selectZaSysEventList(zaSysEvent);
        return success(list);
    }

    /**
     * 分页查询告警事件列表
     */
    @ApiOperation("分页查询告警事件列表")
    @PreAuthorize("@ss.hasPermi('sys:event:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysEvent zaSysEvent)
    {
        startPage();
        List<ZaSysEvent> list = zaSysEventService.selectZaSysEventList(zaSysEvent);
        return getDataTable(list);
    }

    /**
     * 导出告警事件列表
     */
    @ApiOperation("导出告警事件列表")
    @PreAuthorize("@ss.hasPermi('sys:event:export')")
    @Log(title = "告警事件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysEvent zaSysEvent)
    {
        List<ZaSysEvent> list = zaSysEventService.selectZaSysEventList(zaSysEvent);
        ExcelUtil<ZaSysEvent> util = new ExcelUtil<ZaSysEvent>(ZaSysEvent.class);
        util.exportExcel(response, list, "告警事件数据");
    }

    /**
     * 获取告警事件详细信息
     */
    @ApiOperation("获取告警事件详细信息")
    @ApiImplicitParam(name = "id", value = "告警事件主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:event:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(zaSysEventService.selectZaSysEventById(id));
    }

    /**
     * 新增告警事件
     */
    @ApiOperation("新增告警事件")
    @PreAuthorize("@ss.hasPermi('sys:event:add')")
    @Log(title = "告警事件", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysEvent zaSysEvent)
    {
        zaSysEvent.setCreateBy(getUsername());
        return toAjax(zaSysEventService.insertZaSysEvent(zaSysEvent));
    }

    /**
     * 修改告警事件
     */
    @ApiOperation("修改告警事件")
    @PreAuthorize("@ss.hasPermi('sys:event:edit')")
    @Log(title = "告警事件", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysEvent zaSysEvent)
    {
        zaSysEvent.setUpdateBy(getUsername());
        return toAjax(zaSysEventService.updateZaSysEvent(zaSysEvent));
    }

    /**
     * 删除告警事件
     */
    @ApiOperation("删除告警事件")
    @ApiImplicitParam(name = "ids", value = "告警事件主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:event:remove')")
    @Log(title = "告警事件", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysEventService.deleteZaSysEventByIds(ids));
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public R purge()
    {
        zaSysEventService.purge();
        return toAjax(true);
    }
}
