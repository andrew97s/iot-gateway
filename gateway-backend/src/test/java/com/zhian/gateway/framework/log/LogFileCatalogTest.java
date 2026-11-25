package com.zhian.gateway.framework.log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 直接运行：javac 后 java com.zhian.gateway.framework.log.LogFileCatalogTest
 */
public class LogFileCatalogTest
{
    private static int failed;

    public static void main(String[] args) throws Exception
    {
        File dir = new File("target/log-catalog-test");
        if (dir.exists())
        {
            File[] old = dir.listFiles();
            if (old != null)
            {
                for (File file : old)
                {
                    file.delete();
                }
            }
        }
        else
        {
            dir.mkdirs();
        }
        write(new File(dir, "gateway-2026-11-23.0.log"),
                "2026-11-23 07:07:00.100 [main] INFO c.z.Boot - [start,1] - started\n"
                        + "2026-11-23 12:00:00.100 [main] WARN c.z.Boot - [check,2] - disk low\n"
                        + "2026-11-23 23:59:59.100 [main] ERROR c.z.Boot - [fail,3] - boom\n");
        write(new File(dir, "gateway-2026-11-25.0.log"),
                "2026-11-25 00:00:00.100 [main] INFO c.z.Boot - [start,1] - morning\n"
                        + "2026-11-25 14:54:24.100 [sip] INFO c.z.Sip - [on,4] - sip event MESSAGE\n");
        write(new File(dir, "sys-error.2026-11-24.log"),
                "2026-11-24 00:00:00.100 [main] ERROR c.z.Old - [x,1] - legacy error\n"
                        + "2026-11-24 23:59:25.100 [main] ERROR c.z.Old - [x,2] - legacy end\n");
        write(new File(dir, "notes.txt"), "2026-11-25 00:00:00 not a log\n");

        LogFileCatalog catalog = new LogFileCatalog(dir);
        List<LogFileItem> all = catalog.list(null, null, null);
        expect(all.size() == 3, "only .log files are listed, got " + all.size());
        expect("gateway-2026-11-25.0.log".equals(all.get(0).getFileName()), "newest file first");
        expect("2026-11-25 00:00:00".equals(all.get(0).getStartTime()), "start from first line");
        expect("2026-11-25 14:54:24".equals(all.get(0).getEndTime()), "end from last line");
        expect("2026-11-23 07:07:00".equals(find(all, "gateway-2026-11-23.0.log").getStartTime()), "partial day start");
        expect(find(all, "gateway-2026-11-23.0.log").getSizeText().endsWith("B"), "size text");

        List<LogFileItem> byName = catalog.list("sys-error", null, null);
        expect(byName.size() == 1 && byName.get(0).getFileName().startsWith("sys-error"), "filename keyword");

        List<LogFileItem> byBody = catalog.list("sip event", null, null);
        expect(byBody.size() == 1 && "gateway-2026-11-25.0.log".equals(byBody.get(0).getFileName()), "content keyword");

        List<LogFileItem> byTime = catalog.list(null, "2026-11-25 10:00:00", "2026-11-25 16:00:00");
        expect(byTime.size() == 1 && "gateway-2026-11-25.0.log".equals(byTime.get(0).getFileName()), "time overlap");

        List<LogFileItem> outside = catalog.list(null, "2026-11-26 00:00:00", "2026-11-26 23:59:59");
        expect(outside.isEmpty(), "outside range excluded");

        LogFileContent content = catalog.read("gateway-2026-11-23.0.log", null);
        expect(content.getContent().contains("INFO") && content.getContent().contains("WARN") && content.getContent().contains("ERROR"),
                "one file keeps info warn error");
        LogFileContent filtered = catalog.read("gateway-2026-11-23.0.log", "ERROR");
        expect(filtered.getLineCount() == 1 && filtered.getContent().contains("boom"), "content keyword filter");

        boolean rejected = false;
        try
        {
            catalog.resolve("../secret.log");
        }
        catch (IllegalArgumentException e)
        {
            rejected = true;
        }
        expect(rejected, "path traversal rejected");

        expect("1.08MB".equals(LogFileCatalog.formatSize((long) (1.08 * 1024 * 1024))), "mb format");
        expect(LogFileCatalog.formatSize(16 * 1024 + 890).startsWith("16."), "kb format");

        if (failed > 0)
        {
            System.exit(1);
        }
        System.out.println("LogFileCatalogTest ok");
    }

    private static LogFileItem find(List<LogFileItem> items, String name)
    {
        for (LogFileItem item : items)
        {
            if (name.equals(item.getFileName()))
            {
                return item;
            }
        }
        return null;
    }

    private static void write(File file, String text) throws Exception
    {
        FileOutputStream out = new FileOutputStream(file);
        try
        {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        finally
        {
            out.close();
        }
    }

    private static void expect(boolean condition, String message)
    {
        if (!condition)
        {
            failed++;
            System.out.println("FAIL " + message);
        }
    }
}
