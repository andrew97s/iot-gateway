package com.zhian.gateway.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.sys.domain.ZaSysUpstream;
import com.zhian.gateway.sys.mapper.ZaSysUpstreamMapper;
import com.zhian.gateway.third.gw.MessageSyncHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 上级平台连接配置
 *
 * 支持多上级平台；配置增删改/启停后推送通道热重载，立即生效。
 */
@Api("上级平台配置")
@RestController
@RequestMapping("/sys/upstream")
public class ZaSysUpstreamController extends BaseController {

    @Autowired
    private ZaSysUpstreamMapper mapper;

    @Autowired
    private MessageSyncHandler messageSyncHandler;

    private LambdaQueryWrapper<ZaSysUpstream> buildQuery(ZaSysUpstream query) {
        return Wrappers.lambdaQuery(ZaSysUpstream.class)
                .like(StringUtils.isNotEmpty(query.getName()), ZaSysUpstream::getName, query.getName())
                .eq(StringUtils.isNotEmpty(query.getPushType()), ZaSysUpstream::getPushType, query.getPushType())
                .eq(StringUtils.isNotEmpty(query.getStatus()), ZaSysUpstream::getStatus, query.getStatus())
                .orderByDesc(ZaSysUpstream::getId);
    }

    @ApiOperation("分页查询上级平台")
    @PreAuthorize("@ss.hasPermi('sys:upstream:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysUpstream query) {
        startPage();
        List<ZaSysUpstream> list = mapper.selectList(buildQuery(query));
        return getDataTable(list);
    }

    @ApiOperation("不分页查询上级平台")
    @GetMapping("/select")
    public AjaxResult select(ZaSysUpstream query) {
        return success(mapper.selectList(buildQuery(query)));
    }

    @ApiOperation("上级平台运行时状态")
    @GetMapping("/status")
    public AjaxResult runtimeStatus() {
        return success(messageSyncHandler.upstreamRuntimeStatus());
    }

    @ApiOperation("上级平台详情")
    @PreAuthorize("@ss.hasPermi('sys:upstream:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(mapper.selectById(id));
    }

    @ApiOperation("新增上级平台")
    @PreAuthorize("@ss.hasPermi('sys:upstream:add')")
    @Log(title = "上级平台", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaSysUpstream entity) {
        if (checkCodeExists(entity.getCode(), null)) {
            return error("平台代码已存在：" + entity.getCode());
        }
        entity.setCreateTime(new Date());
        if (StringUtils.isEmpty(entity.getStatus())) {
            entity.setStatus("1");
        }
        int rows = mapper.insert(entity);
        messageSyncHandler.reloadUpstreams();
        return toAjax(rows);
    }

    @ApiOperation("修改上级平台")
    @PreAuthorize("@ss.hasPermi('sys:upstream:edit')")
    @Log(title = "上级平台", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ZaSysUpstream entity) {
        if (checkCodeExists(entity.getCode(), entity.getId())) {
            return error("平台代码已存在：" + entity.getCode());
        }
        entity.setUpdateTime(new Date());
        int rows = mapper.updateById(entity);
        messageSyncHandler.reloadUpstreams();
        return toAjax(rows);
    }

    @ApiOperation("删除上级平台")
    @PreAuthorize("@ss.hasPermi('sys:upstream:remove')")
    @Log(title = "上级平台", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        int rows = mapper.deleteBatchIds(Arrays.asList(ids));
        messageSyncHandler.reloadUpstreams();
        return toAjax(rows);
    }

    @ApiOperation("测试上级平台连通性")
    @PreAuthorize("@ss.hasPermi('sys:upstream:edit')")
    @PostMapping("/test")
    public AjaxResult test(@RequestBody ZaSysUpstream entity) {
        MessageSyncHandler.UpstreamPushResult result = messageSyncHandler.testUpstream(entity);
        return result.isSuccess()
                ? success("连接成功：" + result.getTarget())
                : error("连接失败：" + StringUtils.nvl(result.getError(), "未知错误"));
    }

    private boolean checkCodeExists(String code, Long excludeId) {
        if (StringUtils.isEmpty(code)) {
            return false;
        }
        return mapper.selectCount(Wrappers.lambdaQuery(ZaSysUpstream.class)
                .eq(ZaSysUpstream::getCode, code)
                .ne(excludeId != null, ZaSysUpstream::getId, excludeId)) > 0;
    }
}
