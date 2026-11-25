package com.zhian.gateway.framework.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * 把已经格式化的日志行交给实时日志页面，本身不写文件。
 */
public class WebLogAppender extends AppenderBase<ILoggingEvent>
{
    private String pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    private PatternLayout layout;

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    @Override
    public void start()
    {
        layout = new PatternLayout();
        layout.setContext(getContext());
        layout.setPattern(pattern);
        layout.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        if (!isStarted() || layout == null || eventObject == null)
        {
            return;
        }
        try
        {
            LogStreamHub.publish(layout.doLayout(eventObject));
        }
        catch (Exception ignored)
        {
        }
    }
}
