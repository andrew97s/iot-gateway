package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysDataset;
import com.zhian.gateway.sys.service.IZaSysDatasetService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据集Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("数据集")
@RestController
@RequestMapping("/sys/dataset")
public class ZaSysDatasetController extends BaseController
{
    @Autowired
    private IZaSysDatasetService zaSysDatasetService;

    /**
     * 不分页查询数据集列表
     */
    @ApiOperation("不分页查询数据集列表")
    @GetMapping("/select")
    public R select(ZaSysDataset zaSysDataset)
    {
        List<ZaSysDataset> list = zaSysDatasetService.selectZaSysDatasetList(zaSysDataset);
        return success(list);
    }

    /**
     * 分页查询数据集列表
     */
    @ApiOperation("分页查询数据集列表")
    @PreAuthorize("@ss.hasPermi('sys:dataset:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysDataset zaSysDataset)
    {
        startPage();
        List<ZaSysDataset> list = zaSysDatasetService.selectZaSysDatasetList(zaSysDataset);
        return getDataTable(list);
    }

    /**
     * 导出数据集列表
     */
    @ApiOperation("导出数据集列表")
    @PreAuthorize("@ss.hasPermi('sys:dataset:export')")
    @Log(title = "数据集", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysDataset zaSysDataset)
    {
        List<ZaSysDataset> list = zaSysDatasetService.selectZaSysDatasetList(zaSysDataset);
        ExcelUtil<ZaSysDataset> util = new ExcelUtil<ZaSysDataset>(ZaSysDataset.class);
        util.exportExcel(response, list, "数据集数据");
    }

    /**
     * 获取数据集详细信息
     */
    @ApiOperation("获取数据集详细信息")
    @ApiImplicitParam(name = "id", value = "数据集主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:dataset:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(zaSysDatasetService.selectZaSysDatasetById(id));
    }

    /**
     * 新增数据集
     */
    @ApiOperation("新增数据集")
    @PreAuthorize("@ss.hasPermi('sys:dataset:add')")
    @Log(title = "数据集", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysDataset zaSysDataset)
    {
        zaSysDataset.setCreateBy(getUsername());
        return toAjax(zaSysDatasetService.insertZaSysDataset(zaSysDataset));
    }

    /**
     * 修改数据集
     */
    @ApiOperation("修改数据集")
    @PreAuthorize("@ss.hasPermi('sys:dataset:edit')")
    @Log(title = "数据集", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysDataset zaSysDataset)
    {
        zaSysDataset.setUpdateBy(getUsername());
        return toAjax(zaSysDatasetService.updateZaSysDataset(zaSysDataset));
    }

    /**
     * 删除数据集
     */
    @ApiOperation("删除数据集")
    @ApiImplicitParam(name = "ids", value = "数据集主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:dataset:remove')")
    @Log(title = "数据集", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysDatasetService.deleteZaSysDatasetByIds(ids));
    }
}
