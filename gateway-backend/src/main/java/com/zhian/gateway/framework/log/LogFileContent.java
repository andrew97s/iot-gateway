package com.zhian.gateway.framework.log;

/**
 * 历史日志查看结果。内容只保留末尾若干行，避免一次把整天的文件读进内存。
 */
public class LogFileContent
{
    private String fileName;

    private String content;

    private boolean truncated;

    private int lineCount;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public void setTruncated(boolean truncated)
    {
        this.truncated = truncated;
    }

    public int getLineCount()
    {
        return lineCount;
    }

    public void setLineCount(int lineCount)
    {
        this.lineCount = lineCount;
    }
}