package com.zhian.gateway.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaDeviceType;
import com.zhian.gateway.sys.mapper.ZaDeviceTypeMapper;
import com.zhian.gateway.sys.service.TypeMappingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 标准设备类型配置
 */
@Api("标准设备类型")
@RestController
@RequestMapping("/sys/device-type")
public class ZaDeviceTypeController extends BaseController {

    @Autowired
    private ZaDeviceTypeMapper mapper;

    @Autowired
    private TypeMappingService typeMappingService;

    private LambdaQueryWrapper<ZaDeviceType> buildQuery(ZaDeviceType query) {
        return Wrappers.lambdaQuery(ZaDeviceType.class)
                .like(StringUtils.isNotEmpty(query.getName()), ZaDeviceType::getName, query.getName())
                .like(StringUtils.isNotEmpty(query.getCode()), ZaDeviceType::getCode, query.getCode())
                .eq(StringUtils.isNotEmpty(query.getStatus()), ZaDeviceType::getStatus, query.getStatus())
                .like(StringUtils.isNotEmpty(query.getPfCode()), ZaDeviceType::getAliases, query.getPfCode())
                .orderByDesc(ZaDeviceType::getId);
    }

    @ApiOperation("分页查询设备类型")
    @PreAuthorize("@ss.hasPermi('sys:devicetype:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaDeviceType query) {
        startPage();
        List<ZaDeviceType> list = mapper.selectList(buildQuery(query));
        return getDataTable(list);
    }

    @ApiOperation("不分页查询设备类型")
    @GetMapping("/select")
    public AjaxResult select(ZaDeviceType query) {
        return success(mapper.selectList(buildQuery(query)));
    }

    @ApiOperation("设备类型详情")
    @PreAuthorize("@ss.hasPermi('sys:devicetype:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(mapper.selectById(id));
    }

    @ApiOperation("新增设备类型")
    @PreAuthorize("@ss.hasPermi('sys:devicetype:add')")
    @Log(title = "设备类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaDeviceType entity) {
        if (checkCodeExists(entity.getCode(), null)) {
            return error("类型编码已存在：" + entity.getCode());
        }
        entity.setCreateTime(new Date());
        if (StringUtils.isEmpty(entity.getStatus())) {
            entity.setStatus("1");
        }
        int rows = mapper.insert(entity);
        typeMappingService.reload();
        return toAjax(rows);
    }

    @ApiOperation("修改设备类型")
    @PreAuthorize("@ss.hasPermi('sys:devicetype:edit')")
    @Log(title = "设备类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ZaDeviceType entity) {
        if (checkCodeExists(entity.getCode(), entity.getId())) {
            return error("类型编码已存在：" + entity.getCode());
        }
        entity.setUpdateTime(new Date());
        int rows = mapper.updateById(entity);
        typeMappingService.reload();
        return toAjax(rows);
    }

    @ApiOperation("删除设备类型")
    @PreAuthorize("@ss.hasPermi('sys:devicetype:remove')")
    @Log(title = "设备类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        int rows = mapper.deleteBatchIds(Arrays.asList(ids));
        typeMappingService.reload();
        return toAjax(rows);
    }

    private boolean checkCodeExists(String code, Long excludeId) {
        if (StringUtils.isEmpty(code)) {
            return false;
        }
        return mapper.selectCount(Wrappers.lambdaQuery(ZaDeviceType.class)
                .eq(ZaDeviceType::getCode, code)
                .ne(excludeId != null, ZaDeviceType::getId, excludeId)) > 0;
    }
}
