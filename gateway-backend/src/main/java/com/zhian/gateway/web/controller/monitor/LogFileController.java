package com.zhian.gateway.web.controller.monitor;

import com.zhian.gateway.common.constant.HttpStatus;
import com.zhian.gateway.common.core.domain.R;
import com.zhian.gateway.common.core.page.PageDomain;
import com.zhian.gateway.common.core.page.TableDataInfo;
import com.zhian.gateway.common.core.page.TableSupport;
import com.zhian.gateway.framework.log.LogFileItem;
import com.zhian.gateway.framework.log.LogFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 运行日志：历史文件与当前正在写入的文件。
 */
@RestController
@RequestMapping("/monitor/logfile")
public class LogFileController
{
    @Autowired
    private LogFileService logFileService;

    @PreAuthorize("@ss.hasPermi('monitor:logfile:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "beginTime", required = false) String beginTime,
                              @RequestParam(value = "endTime", required = false) String endTime)
    {
        List<LogFileItem> all = logFileService.list(keyword, beginTime, endTime);
        PageDomain page = TableSupport.buildPageRequest();
        int pageNum = page.getPageNum() == null || page.getPageNum() < 1 ? 1 : page.getPageNum();
        int pageSize = page.getPageSize() == null || page.getPageSize() < 1 ? 10 : page.getPageSize();
        if (pageSize > 200)
        {
            pageSize = 200;
        }
        int from = (pageNum - 1) * pageSize;
        List<LogFileItem> rows = new ArrayList<LogFileItem>();
        if (from < all.size())
        {
            int to = Math.min(all.size(), from + pageSize);
            rows.addAll(all.subList(from, to));
        }
        TableDataInfo data = new TableDataInfo();
        data.setCode(HttpStatus.SUCCESS);
        data.setMsg("查询成功");
        data.setRows(rows);
        data.setTotal(all.size());
        return data;
    }

    @PreAuthorize("@ss.hasPermi('monitor:logfile:list')")
    @GetMapping("/current")
    public R current()
    {
        return R.success(logFileService.current());
    }

    @PreAuthorize("@ss.hasPermi('monitor:logfile:list')")
    @GetMapping("/content")
    public R content(@RequestParam("fileName") String fileName,
                     @RequestParam(value = "keyword", required = false) String keyword)
    {
        return R.success(logFileService.read(fileName, keyword));
    }

    @PreAuthorize("@ss.hasPermi('monitor:logfile:list')")
    @GetMapping("/download")
    public void download(@RequestParam("fileName") String fileName, HttpServletResponse response) throws IOException
    {
        logFileService.download(fileName, response);
    }

    @PreAuthorize("@ss.hasPermi('monitor:logfile:list')")
    @PostMapping("/download")
    public void downloadBatch(@RequestBody List<String> fileNames, HttpServletResponse response) throws IOException
    {
        logFileService.downloadZip(fileNames, response);
    }
}