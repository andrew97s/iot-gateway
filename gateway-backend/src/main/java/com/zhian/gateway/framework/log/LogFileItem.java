package com.zhian.gateway.framework.log;

/**
 * 历史日志列表中的一个日志文件。
 */
public class LogFileItem
{
    private String fileName;

    private long size;

    private String sizeText;

    private String startTime;

    private String endTime;

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public String getSizeText()
    {
        return sizeText;
    }

    public void setSizeText(String sizeText)
    {
        this.sizeText = sizeText;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }
}