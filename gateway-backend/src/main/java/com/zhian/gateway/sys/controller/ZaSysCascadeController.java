package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysCascade;
import com.zhian.gateway.sys.service.IZaSysCascadeService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 级联平台Controller
 * 
 * @author yepanpan
 * @date 2024-05-13
 */
@Api("级联平台")
@RestController
@RequestMapping("/sys/cascade")
public class ZaSysCascadeController extends BaseController
{
    @Autowired
    private IZaSysCascadeService zaSysCascadeService;

    /**
     * 不分页查询级联平台列表
     */
    @ApiOperation("不分页查询级联平台列表")
    @GetMapping("/select")
    public R select(ZaSysCascade zaSysCascade)
    {
        List<ZaSysCascade> list = zaSysCascadeService.selectZaSysCascadeList(zaSysCascade);
        return success(list);
    }

    /**
     * 分页查询级联平台列表
     */
    @ApiOperation("分页查询级联平台列表")
    @PreAuthorize("@ss.hasPermi('sys:cascade:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysCascade zaSysCascade)
    {
        startPage();
        List<ZaSysCascade> list = zaSysCascadeService.selectZaSysCascadeList(zaSysCascade);
        return getDataTable(list);
    }

    /**
     * 导出级联平台列表
     */
    @ApiOperation("导出级联平台列表")
    @PreAuthorize("@ss.hasPermi('sys:cascade:export')")
    @Log(title = "级联平台", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysCascade zaSysCascade)
    {
        List<ZaSysCascade> list = zaSysCascadeService.selectZaSysCascadeList(zaSysCascade);
        ExcelUtil<ZaSysCascade> util = new ExcelUtil<ZaSysCascade>(ZaSysCascade.class);
        util.exportExcel(response, list, "级联平台数据");
    }

    /**
     * 获取级联平台详细信息
     */
    @ApiOperation("获取级联平台详细信息")
    @ApiImplicitParam(name = "id", value = "级联平台主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:cascade:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(zaSysCascadeService.selectZaSysCascadeById(id));
    }

    /**
     * 新增级联平台
     */
    @ApiOperation("新增级联平台")
    @PreAuthorize("@ss.hasPermi('sys:cascade:add')")
    @Log(title = "级联平台", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysCascade zaSysCascade)
    {
        zaSysCascade.setCreateBy(getUsername());
        return toAjax(zaSysCascadeService.insertZaSysCascade(zaSysCascade));
    }

    /**
     * 修改级联平台
     */
    @ApiOperation("修改级联平台")
    @PreAuthorize("@ss.hasPermi('sys:cascade:edit')")
    @Log(title = "级联平台", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysCascade zaSysCascade)
    {
        zaSysCascade.setUpdateBy(getUsername());
        return toAjax(zaSysCascadeService.updateZaSysCascade(zaSysCascade));
    }

    /**
     * 删除级联平台
     */
    @ApiOperation("删除级联平台")
    @ApiImplicitParam(name = "ids", value = "级联平台主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:cascade:remove')")
    @Log(title = "级联平台", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysCascadeService.deleteZaSysCascadeByIds(ids));
    }
}
