package com.zhian.gateway.framework.log;

import com.zhian.gateway.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 历史日志的列表、查看和下载。
 */
@Service
public class LogFileService
{
    private static final int MAX_DOWNLOAD = 50;

    private final LogFileCatalog catalog;

    public LogFileService(@Value("${logging.file.path:logs}") String logPath)
    {
        this.catalog = new LogFileCatalog(new File(logPath));
        final LogFileCatalog files = this.catalog;
        LogStreamHub.setSnapshot(new LogStreamHub.Snapshot()
        {
            @Override
            public List<String> lines()
            {
                return files.tailLatest(LogStreamHub.RECENT_LIMIT);
            }
        });
    }

    public List<LogFileItem> list(String keyword, String beginTime, String endTime)
    {
        return catalog.list(keyword, beginTime, endTime);
    }

    public LogFileItem current()
    {
        return catalog.current();
    }

    public LogFileContent read(String fileName, String keyword)
    {
        try
        {
            return catalog.read(fileName, keyword);
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException(e.getMessage());
        }
    }

    public void download(String fileName, HttpServletResponse response) throws IOException
    {
        File file;
        try
        {
            file = catalog.resolve(fileName);
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException(e.getMessage());
        }
        writeAttachment(response, file.getName(), "application/octet-stream");
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try
        {
            copy(in, response.getOutputStream());
        }
        finally
        {
            in.close();
        }
    }

    public void downloadZip(List<String> fileNames, HttpServletResponse response) throws IOException
    {
        if (fileNames == null || fileNames.isEmpty())
        {
            throw new ServiceException("请选择要下载的日志文件");
        }
        if (fileNames.size() > MAX_DOWNLOAD)
        {
            throw new ServiceException("一次最多下载 " + MAX_DOWNLOAD + " 个日志文件");
        }
        List<File> files = new ArrayList<File>();
        for (String fileName : fileNames)
        {
            try
            {
                files.add(catalog.resolve(fileName));
            }
            catch (IllegalArgumentException e)
            {
                throw new ServiceException(e.getMessage());
            }
        }
        if (files.size() == 1)
        {
            download(files.get(0).getName(), response);
            return;
        }
        writeAttachment(response, "gateway-logs.zip", "application/zip");
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()));
        try
        {
            for (File file : files)
            {
                zip.putNextEntry(new ZipEntry(file.getName()));
                InputStream in = new BufferedInputStream(new FileInputStream(file));
                try
                {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) >= 0)
                    {
                        zip.write(buf, 0, n);
                    }
                }
                finally
                {
                    in.close();
                }
                zip.closeEntry();
            }
        }
        finally
        {
            zip.close();
        }
    }

    private static void writeAttachment(HttpServletResponse response, String fileName, String contentType) throws IOException
    {
        response.setContentType(contentType);
        response.setCharacterEncoding("utf-8");
        com.zhian.gateway.common.utils.file.FileUtils.setAttachmentResponseHeader(response, fileName);
    }

    private static void copy(InputStream in, java.io.OutputStream out) throws IOException
    {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0)
        {
            out.write(buf, 0, n);
        }
        out.flush();
    }
}