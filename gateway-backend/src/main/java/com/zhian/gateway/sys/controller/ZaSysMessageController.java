package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.AjaxResult;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 接入消息Controller
 * 
 * @author yepanpan
 * @date 2024-07-10
 */
@Api("接入消息")
@RestController
@RequestMapping("/sys/message")
public class ZaSysMessageController extends BaseController
{
    @Autowired
    private IZaSysMessageService zaSysMessageService;

    /**
     * 不分页查询接入消息列表
     */
    @ApiOperation("不分页查询接入消息列表")
    @GetMapping("/select")
    public AjaxResult select(ZaSysMessage zaSysMessage)
    {
        List<ZaSysMessage> list = zaSysMessageService.selectZaSysMessageList(zaSysMessage);
        return success(list);
    }

    /**
     * 分页查询接入消息列表
     */
    @ApiOperation("分页查询接入消息列表")
    @PreAuthorize("@ss.hasPermi('sys:message:list')")
    @GetMapping("/list")
    public TableDataInfo list(ZaSysMessage zaSysMessage)
    {
        startPage();
        List<ZaSysMessage> list = zaSysMessageService.selectZaSysMessageList(zaSysMessage);
        return getDataTable(list);
    }

    /**
     * 导出接入消息列表
     */
    @ApiOperation("导出接入消息列表")
    @PreAuthorize("@ss.hasPermi('sys:message:export')")
    @Log(title = "接入消息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ZaSysMessage zaSysMessage)
    {
        List<ZaSysMessage> list = zaSysMessageService.selectZaSysMessageList(zaSysMessage);
        ExcelUtil<ZaSysMessage> util = new ExcelUtil<ZaSysMessage>(ZaSysMessage.class);
        util.exportExcel(response, list, "接入消息数据");
    }

    /**
     * 获取接入消息详细信息
     */
    @ApiOperation("获取接入消息详细信息")
    @ApiImplicitParam(name = "id", value = "接入消息主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:message:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(zaSysMessageService.selectZaSysMessageById(id));
    }

    /**
     * 某条消息的推送记录（每次手动推送一条）
     */
    @ApiOperation("消息推送记录")
    @PreAuthorize("@ss.hasPermi('sys:message:list')")
    @GetMapping("/{messageId}/push-logs")
    public AjaxResult pushLogs(@PathVariable Long messageId)
    {
        return success(zaSysMessageService.selectPushLogsByMessageId(messageId));
    }

    /**
     * 新增接入消息
     */
    @ApiOperation("新增接入消息")
    @PreAuthorize("@ss.hasPermi('sys:message:add')")
    @Log(title = "接入消息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ZaSysMessage zaSysMessage)
    {
        zaSysMessage.setCreateBy(getUsername());
        return toAjax(zaSysMessageService.insertZaSysMessage(zaSysMessage));
    }

    /**
     * 修改接入消息
     */
    @ApiOperation("修改接入消息")
    @PreAuthorize("@ss.hasPermi('sys:message:edit')")
    @Log(title = "接入消息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ZaSysMessage zaSysMessage)
    {
        zaSysMessage.setUpdateBy(getUsername());
        return toAjax(zaSysMessageService.updateZaSysMessage(zaSysMessage));
    }

    /**
     * 删除接入消息
     */
    @ApiOperation("删除接入消息")
    @ApiImplicitParam(name = "ids", value = "接入消息主键", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "接入消息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysMessageService.deleteZaSysMessageByIds(ids));
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public AjaxResult purge()
    {
        zaSysMessageService.purge();
        return toAjax(true);
    }

    /**
     * 重试发送消息
     */
    @ApiOperation("重试发送消息")
    @PreAuthorize("@ss.hasPermi('sys:message:edit')")
    @PostMapping("/retry/{id}")
    public AjaxResult retry(@PathVariable("id") Long id)
    {
        boolean ok = zaSysMessageService.retryMessage(id);
        return ok ? success("推送成功") : error("推送失败，请检查网关推送配置");
    }

    /**
     * 按平台统计消息数量
     */
    @ApiOperation("按平台统计消息数量")
    @PreAuthorize("@ss.hasPermi('sys:message:list')")
    @GetMapping("/platform-stats")
    public AjaxResult platformStats()
    {
        return success(zaSysMessageService.countByPlatform());
    }
}
