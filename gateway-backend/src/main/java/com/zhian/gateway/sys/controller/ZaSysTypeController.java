package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysType;
import com.zhian.gateway.sys.service.IZaSysTypeService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 设备类型Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("设备类型")
@RestController
@RequestMapping("/sys/type")
public class ZaSysTypeController extends BaseController
{
    @Autowired
    private IZaSysTypeService ZaSysTypeService;

    /**
     * 不分页查询设备类型列表
     */
    @ApiOperation("不分页查询设备类型列表")
    @GetMapping("/select")
    public R select(ZaSysType zaSysType)
    {
        List<ZaSysType> list = ZaSysTypeService.selectZaSysTypeList(zaSysType);
        return success(list);
    }

    /**
     * 分页查询设备类型列表
     */
    @ApiOperation("分页查询设备类型列表")
    @PreAuthorize("@ss.hasPermi('sys:type:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysType zaSysType)
    {
        List<ZaSysType> list = ZaSysTypeService.selectZaSysTypeList(zaSysType);
        return getDataTable(list);
    }

    /**
     * 导出设备类型列表
     */
    @ApiOperation("导出设备类型列表")
    @PreAuthorize("@ss.hasPermi('sys:type:export')")
    @Log(title = "设备类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysType zaSysType)
    {
        List<ZaSysType> list = ZaSysTypeService.selectZaSysTypeList(zaSysType);
        ExcelUtil<ZaSysType> util = new ExcelUtil<ZaSysType>(ZaSysType.class);
        util.exportExcel(response, list, "设备类型数据");
    }

    /**
     * 获取设备类型详细信息
     */
    @ApiOperation("获取设备类型详细信息")
    @ApiImplicitParam(name = "id", value = "设备类型主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:type:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(ZaSysTypeService.selectZaSysTypeById(id));
    }

    /**
     * 新增设备类型
     */
    @ApiOperation("新增设备类型")
    @PreAuthorize("@ss.hasPermi('sys:type:add')")
    @Log(title = "设备类型", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysType zaSysType)
    {
        zaSysType.setCreateBy(getUsername());
        return toAjax(ZaSysTypeService.insertZaSysType(zaSysType));
    }

    /**
     * 修改设备类型
     */
    @ApiOperation("修改设备类型")
    @PreAuthorize("@ss.hasPermi('sys:type:edit')")
    @Log(title = "设备类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysType zaSysType)
    {
        zaSysType.setUpdateBy(getUsername());
        return toAjax(ZaSysTypeService.updateZaSysType(zaSysType));
    }

    /**
     * 删除设备类型
     */
    @ApiOperation("删除设备类型")
    @ApiImplicitParam(name = "ids", value = "设备类型主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:type:remove')")
    @Log(title = "设备类型", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(ZaSysTypeService.deleteZaSysTypeByIds(ids));
    }
}
