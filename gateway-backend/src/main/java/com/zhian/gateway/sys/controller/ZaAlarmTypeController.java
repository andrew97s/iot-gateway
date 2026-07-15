package com.zhian.gateway.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaAlarmType;
import com.zhian.gateway.sys.mapper.ZaAlarmTypeMapper;
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
 * 标准告警类型配置
 */
@Api("标准告警类型")
@RestController
@RequestMapping("/sys/alarm-type")
public class ZaAlarmTypeController extends BaseController {

    @Autowired
    private ZaAlarmTypeMapper mapper;

    @Autowired
    private TypeMappingService typeMappingService;

    private LambdaQueryWrapper<ZaAlarmType> buildQuery(ZaAlarmType query) {
        return Wrappers.lambdaQuery(ZaAlarmType.class)
                .like(StringUtils.isNotEmpty(query.getName()), ZaAlarmType::getName, query.getName())
                .like(StringUtils.isNotEmpty(query.getCode()), ZaAlarmType::getCode, query.getCode())
                .eq(StringUtils.isNotEmpty(query.getStatus()), ZaAlarmType::getStatus, query.getStatus())
                .like(StringUtils.isNotEmpty(query.getPfCode()), ZaAlarmType::getAliases, query.getPfCode())
                .orderByDesc(ZaAlarmType::getId);
    }

    @ApiOperation("分页查询告警类型")
    @PreAuthorize("@ss.hasPermi('sys:alarmtype:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaAlarmType query) {
        startPage();
        List<ZaAlarmType> list = mapper.selectList(buildQuery(query));
        return getDataTable(list);
    }

    @ApiOperation("不分页查询告警类型")
    @GetMapping("/select")
    public AjaxResult select(ZaAlarmType query) {
        return success(mapper.selectList(buildQuery(query)));
    }

    @ApiOperation("告警类型详情")
    @PreAuthorize("@ss.hasPermi('sys:alarmtype:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(mapper.selectById(id));
    }

    @ApiOperation("新增告警类型")
    @PreAuthorize("@ss.hasPermi('sys:alarmtype:add')")
    @Log(title = "告警类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaAlarmType entity) {
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

    @ApiOperation("修改告警类型")
    @PreAuthorize("@ss.hasPermi('sys:alarmtype:edit')")
    @Log(title = "告警类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ZaAlarmType entity) {
        if (checkCodeExists(entity.getCode(), entity.getId())) {
            return error("类型编码已存在：" + entity.getCode());
        }
        entity.setUpdateTime(new Date());
        int rows = mapper.updateById(entity);
        typeMappingService.reload();
        return toAjax(rows);
    }

    @ApiOperation("删除告警类型")
    @PreAuthorize("@ss.hasPermi('sys:alarmtype:remove')")
    @Log(title = "告警类型", businessType = BusinessType.DELETE)
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
        return mapper.selectCount(Wrappers.lambdaQuery(ZaAlarmType.class)
                .eq(ZaAlarmType::getCode, code)
                .ne(excludeId != null, ZaAlarmType::getId, excludeId)) > 0;
    }
}
