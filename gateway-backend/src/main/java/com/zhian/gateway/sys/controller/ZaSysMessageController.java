package com.zhian.gateway.sys.controller;

import com.zhian.gateway.common.annotation.Log;
import com.zhian.gateway.common.core.controller.BaseController;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.enums.BusinessType;
import com.zhian.gateway.common.utils.poi.ExcelUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhian.gateway.sys.domain.ZaSysMessage;
import com.zhian.gateway.sys.domain.ZaSysMessageLog;
import com.zhian.gateway.sys.domain.ZaSysUpstream;
import com.zhian.gateway.sys.mapper.ZaSysUpstreamMapper;
import com.zhian.gateway.sys.service.IZaSysMessageService;
import com.zhian.gateway.sys.utils.MessageViewHelper;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private ZaSysUpstreamMapper upstreamMapper;

    /**
     * 不分页查询接入消息列表
     */
    @ApiOperation("不分页查询接入消息列表")
    @GetMapping("/select")
    public R select(ZaSysMessage zaSysMessage)
    {
        List<ZaSysMessage> list = zaSysMessageService.selectZaSysMessageList(zaSysMessage);
        enrichMessages(list);
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
        enrichMessages(list);
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
    public R getInfo(@PathVariable("id") Long id)
    {
        ZaSysMessage msg = zaSysMessageService.selectZaSysMessageById(id);
        if (msg != null) {
            List<ZaSysMessageLog> logs = zaSysMessageService.selectPushLogsByMessageId(id);
            MessageViewHelper.enrich(msg, enabledUpstreamCount(), logs);
        }
        return success(msg);
    }

    /**
     * 某条消息的推送记录（每次手动推送一条）
     */
    @ApiOperation("消息推送记录")
    @PreAuthorize("@ss.hasPermi('sys:message:list')")
    @GetMapping("/{messageId}/push-logs")
    public R pushLogs(@PathVariable Long messageId)
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
    public R add(@RequestBody ZaSysMessage zaSysMessage)
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
    public R edit(@RequestBody ZaSysMessage zaSysMessage)
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
    public R remove(@PathVariable Long[] ids)
    {
        return toAjax(zaSysMessageService.deleteZaSysMessageByIds(ids));
    }

    @ApiOperation("清除数据")
    @PreAuthorize("@ss.hasPermi('sys:message:remove')")
    @Log(title = "清除数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/purge")
    public R purge()
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
    public R retry(@PathVariable("id") Long id)
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
    public R platformStats()
    {
        return success(zaSysMessageService.countByPlatform());
    }

    @ApiOperation("今日消息同步摘要")
    @PreAuthorize("@ss.hasPermi('sys:message:list')")
    @GetMapping("/today-stats")
    public R todayStats()
    {
        Map<String, Object> stats = zaSysMessageService.selectTodaySendStats();
        if (stats == null) {
            stats = new HashMap<>();
        }
        long total = toLong(stats.get("total"));
        long sent = toLong(stats.get("sentCount"));
        long failed = toLong(stats.get("failedCount"));
        double rate = total <= 0 ? 100.0 : Math.round(sent * 1000.0 / total) / 10.0;
        stats.put("successRate", rate);
        stats.put("pendingCount", Math.max(0, total - sent - failed) + failed);
        return success(stats);
    }

    @ApiOperation("批量重推失败消息")
    @PreAuthorize("@ss.hasPermi('sys:message:edit')")
    @Log(title = "批量重推消息", businessType = BusinessType.UPDATE)
    @PostMapping("/retry/batch")
    public R retryBatch(@RequestBody Map<String, Object> body)
    {
        Object idsObj = body != null ? body.get("ids") : null;
        int ok = 0, fail = 0;
        if (idsObj instanceof List) {
            for (Object id : (List<?>) idsObj) {
                if (id == null) {
                    continue;
                }
                try {
                    boolean r = zaSysMessageService.retryMessage(Long.valueOf(String.valueOf(id)));
                    if (r) {
                        ok++;
                    } else {
                        fail++;
                    }
                } catch (Exception e) {
                    fail++;
                }
            }
        } else {
            // 未传 ids：重推最近失败消息（最多 50 条）
            com.github.pagehelper.PageHelper.startPage(1, 50, "id desc");
            ZaSysMessage q = new ZaSysMessage();
            q.setSendStatus(ZaSysMessage.SEND_STATUS_FAILED);
            List<ZaSysMessage> list = zaSysMessageService.selectZaSysMessageList(q);
            for (ZaSysMessage m : list) {
                if ("control".equalsIgnoreCase(m.getType())) {
                    continue;
                }
                if (zaSysMessageService.retryMessage(m.getId())) {
                    ok++;
                } else {
                    fail++;
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", ok);
        data.put("failed", fail);
        return success(data);
    }

    private void enrichMessages(List<ZaSysMessage> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        int upstreamTotal = enabledUpstreamCount();
        for (ZaSysMessage msg : list) {
            List<ZaSysMessageLog> logs = zaSysMessageService.selectPushLogsByMessageId(msg.getId());
            // 列表不查推送日志，避免 N+1；徽标按 sendStatus + 上级平台数估算
            try {
                MessageViewHelper.enrich(msg, upstreamTotal, logs);
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    private int enabledUpstreamCount() {
        try {
            Long cnt = upstreamMapper.selectCount(Wrappers.lambdaQuery(ZaSysUpstream.class)
                    .eq(ZaSysUpstream::getStatus, ZaSysUpstream.STATUS_ENABLED));
            return cnt == null ? 0 : cnt.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }
}
