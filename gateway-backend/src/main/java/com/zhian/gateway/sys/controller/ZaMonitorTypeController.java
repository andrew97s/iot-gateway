package com.zhian.gateway.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaMonitorType;
import com.zhian.gateway.sys.mapper.ZaMonitorTypeMapper;
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
 * 标准监测类型配置
 */
@Api("标准监测类型")
@RestController
@RequestMapping("/sys/monitor-type")
public class ZaMonitorTypeController extends BaseController {

    @Autowired
    private ZaMonitorTypeMapper mapper;

    @Autowired
    private TypeMappingService typeMappingService;

    private LambdaQueryWrapper<ZaMonitorType> buildQuery(ZaMonitorType query) {
        return Wrappers.lambdaQuery(ZaMonitorType.class)
                .like(StringUtils.isNotEmpty(query.getName()), ZaMonitorType::getName, query.getName())
                .like(StringUtils.isNotEmpty(query.getCode()), ZaMonitorType::getCode, query.getCode())
                .eq(StringUtils.isNotEmpty(query.getStatus()), ZaMonitorType::getStatus, query.getStatus())
                .eq(StringUtils.isNotEmpty(query.getValueType()), ZaMonitorType::getValueType, query.getValueType())
                .like(StringUtils.isNotEmpty(query.getPfCode()), ZaMonitorType::getAliases, query.getPfCode())
                .orderByDesc(ZaMonitorType::getId);
    }

    @ApiOperation("分页查询监测类型")
    @PreAuthorize("@ss.hasPermi('sys:monitortype:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaMonitorType query) {
        startPage();
        List<ZaMonitorType> list = mapper.selectList(buildQuery(query));
        return getDataTable(list);
    }

    @ApiOperation("不分页查询监测类型")
    @GetMapping("/select")
    public R select(ZaMonitorType query) {
        return success(mapper.selectList(buildQuery(query)));
    }

    @ApiOperation("监测类型详情")
    @PreAuthorize("@ss.hasPermi('sys:monitortype:query')")
    @GetMapping("/{id}")
    public R getInfo(@PathVariable Long id) {
        return success(mapper.selectById(id));
    }

    @ApiOperation("新增监测类型")
    @PreAuthorize("@ss.hasPermi('sys:monitortype:add')")
    @Log(title = "监测类型", businessType = BusinessType.INSERT)
    @PostMapping
    public R add(@RequestBody ZaMonitorType entity) {
        if (checkCodeExists(entity.getCode(), null)) {
            return error("类型编码已存在：" + entity.getCode());
        }
        entity.setCreateTime(new Date());
        if (StringUtils.isEmpty(entity.getStatus())) {
            entity.setStatus("1");
        }
        if (StringUtils.isEmpty(entity.getValueType())) {
            entity.setValueType(ZaMonitorType.VALUE_TYPE_LINEAR);
        }
        int rows = mapper.insert(entity);
        typeMappingService.reload();
        return toAjax(rows);
    }

    @ApiOperation("修改监测类型")
    @PreAuthorize("@ss.hasPermi('sys:monitortype:edit')")
    @Log(title = "监测类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public R edit(@RequestBody ZaMonitorType entity) {
        if (checkCodeExists(entity.getCode(), entity.getId())) {
            return error("类型编码已存在：" + entity.getCode());
        }
        entity.setUpdateTime(new Date());
        int rows = mapper.updateById(entity);
        typeMappingService.reload();
        return toAjax(rows);
    }

    @ApiOperation("删除监测类型")
    @PreAuthorize("@ss.hasPermi('sys:monitortype:remove')")
    @Log(title = "监测类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R remove(@PathVariable Long[] ids) {
        int rows = mapper.deleteBatchIds(Arrays.asList(ids));
        typeMappingService.reload();
        return toAjax(rows);
    }

    private boolean checkCodeExists(String code, Long excludeId) {
        if (StringUtils.isEmpty(code)) {
            return false;
        }
        return mapper.selectCount(Wrappers.lambdaQuery(ZaMonitorType.class)
                .eq(ZaMonitorType::getCode, code)
                .ne(excludeId != null, ZaMonitorType::getId, excludeId)) > 0;
    }
}
