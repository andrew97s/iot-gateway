package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysError;
import com.zhian.gateway.sys.service.IZaSysErrorService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 错误日志Controller
 * 
 * @author yepanpan
 * @date 2024-04-11
 */
@Api("错误日志")
@RestController
@RequestMapping("/sys/error")
public class ZaSysErrorController extends BaseController
{
    @Autowired
    private IZaSysErrorService zaSysErrorService;

    /**
     * 分页查询错误日志列表
     */
    @ApiOperation("分页查询错误日志列表")
    @PreAuthorize("@ss.hasPermi('sys:error:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysError zaSysError)
    {
        startPage();
        List<ZaSysError> list = zaSysErrorService.selectZaSysErrorList(zaSysError);
        return getDataTable(list);
    }

    /**
     * 导出错误日志列表
     */
    @ApiOperation("导出错误日志列表")
    @PreAuthorize("@ss.hasPermi('sys:error:export')")
    @Log(title = "错误日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysError zaSysError)
    {
        List<ZaSysError> list = zaSysErrorService.selectZaSysErrorList(zaSysError);
        ExcelUtil<ZaSysError> util = new ExcelUtil<ZaSysError>(ZaSysError.class);
        util.exportExcel(response, list, "错误日志数据");
    }

    /**
     * 获取错误日志详细信息
     */
    @ApiOperation("获取错误日志详细信息")
    @ApiImplicitParam(name = "id", value = "错误日志主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:error:query')")
    @GetMapping(value = "/{id}")
    public R getInfo(@PathVariable("id") Long id)
    {
        return success(zaSysErrorService.selectZaSysErrorById(id));
    }

    /**
     * 新增错误日志
     */
    @ApiOperation("新增错误日志")
    @PreAuthorize("@ss.hasPermi('sys:error:add')")
    @Log(title = "错误日志", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaSysError zaSysError)
    {
        zaSysError.setCreateBy(getUsername());
        return toAjax(zaSysErrorService.insertZaSysError(zaSysError));
    }

    /**
     * 修改错误日志
     */
    @ApiOperation("修改错误日志")
    @PreAuthorize("@ss.hasPermi('sys:error:edit')")
    @Log(title = "错误日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaSysError zaSysError)
    {
        zaSysError.setUpdateBy(getUsername());
        return toAjax(zaSysErrorService.updateZaSysError(zaSysError));
    }

    /**
     * 删除错误日志
     */
    @ApiOperation("删除错误日志")
    @ApiImplicitParam(name = "ids", value = "错误日志主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:error:remove')")
    @Log(title = "错误日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysErrorService.deleteZaSysErrorByIds(ids));
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public R purge()
    {
        zaSysErrorService.purge();
        return toAjax(true);
    }
}
