package com.zhian.gateway.framework.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扫描日志目录，列出日志文件并读取内容。不依赖 Spring，方便单独验证。
 */
public class LogFileCatalog
{
    public static final int MAX_VIEW_LINES = 2000;

    private static final int TAIL_BYTES = 512 * 1024;

    private static final int HEAD_BYTES = 8192;

    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,180}\\.log$");

    private static final Pattern TIMESTAMP = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2}:\\d{2})");

    private static final Pattern NAME_DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private static final TimeZone SHANGHAI = TimeZone.getTimeZone("Asia/Shanghai");

    private final File directory;

    private final Map<String, Meta> cache = new HashMap<String, Meta>();

    public LogFileCatalog(File directory)
    {
        this.directory = directory;
    }

    public File getDirectory()
    {
        return directory;
    }

    public List<LogFileItem> list(String keyword, String beginTime, String endTime)
    {
        if (directory == null || !directory.isDirectory())
        {
            return new ArrayList<LogFileItem>();
        }
        File[] files = directory.listFiles();
        if (files == null)
        {
            return new ArrayList<LogFileItem>();
        }
        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String begin = blankToNull(beginTime);
        String end = blankToNull(endTime);
        List<LogFileItem> items = new ArrayList<LogFileItem>();
        for (File file : files)
        {
            if (!file.isFile() || !SAFE_NAME.matcher(file.getName()).matches())
            {
                continue;
            }
            LogFileItem item = toItem(file);
            if (!overlaps(item, begin, end))
            {
                continue;
            }
            if (needle.length() > 0 && !matchesKeyword(file, item, needle))
            {
                continue;
            }
            items.add(item);
        }
        Collections.sort(items, new Comparator<LogFileItem>()
        {
            @Override
            public int compare(LogFileItem a, LogFileItem b)
            {
                int byEnd = nullToEmpty(b.getEndTime()).compareTo(nullToEmpty(a.getEndTime()));
                if (byEnd != 0)
                {
                    return byEnd;
                }
                return nullToEmpty(b.getFileName()).compareTo(nullToEmpty(a.getFileName()));
            }
        });
        return items;
    }

    public LogFileItem current()
    {
        List<LogFileItem> items = list(null, null, null);
        if (items.isEmpty())
        {
            return null;
        }
        return items.get(0);
    }

    /**
     * 当前正在写入的 info 日志末尾若干行，供实时日志刚连上时回放。
     */
    public List<String> tailLatest(int maxLines)
    {
        if (maxLines <= 0)
        {
            return new ArrayList<String>();
        }
        File file = newestInfoFile();
        if (file == null)
        {
            return new ArrayList<String>();
        }
        try
        {
            LogFileContent content = read(file.getName(), null);
            List<String> lines = splitLines(content.getContent());
            if (lines.size() <= maxLines)
            {
                return lines;
            }
            return new ArrayList<String>(lines.subList(lines.size() - maxLines, lines.size()));
        }
        catch (RuntimeException e)
        {
            return new ArrayList<String>();
        }
    }

    private File newestInfoFile()
    {
        if (directory == null || !directory.isDirectory())
        {
            return null;
        }
        File[] files = directory.listFiles();
        if (files == null)
        {
            return null;
        }
        File bestInfo = null;
        File best = null;
        for (File file : files)
        {
            if (!file.isFile() || !SAFE_NAME.matcher(file.getName()).matches())
            {
                continue;
            }
            if (best == null || file.lastModified() > best.lastModified())
            {
                best = file;
            }
            if (file.getName().startsWith("sys-info")
                    && (bestInfo == null || file.lastModified() > bestInfo.lastModified()))
            {
                bestInfo = file;
            }
        }
        return bestInfo != null ? bestInfo : best;
    }

    public File resolve(String fileName)
    {
        if (fileName == null || !SAFE_NAME.matcher(fileName).matches() || fileName.contains(".."))
        {
            throw new IllegalArgumentException("日志文件名不合法");
        }
        if (directory == null || !directory.isDirectory())
        {
            throw new IllegalArgumentException("日志目录不存在");
        }
        try
        {
            File dir = directory.getCanonicalFile();
            File file = new File(dir, fileName).getCanonicalFile();
            String dirPath = dir.getPath() + File.separator;
            if (!file.getPath().startsWith(dirPath) || !file.isFile())
            {
                throw new IllegalArgumentException("日志文件不存在");
            }
            return file;
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("日志文件不存在");
        }
    }

    public LogFileContent read(String fileName, String keyword)
    {
        File file = resolve(fileName);
        String needle = keyword == null ? "" : keyword.trim();
        LogFileContent result = new LogFileContent();
        result.setFileName(file.getName());
        try
        {
            if (needle.length() == 0)
            {
                fillTail(file, result);
            }
            else
            {
                fillMatched(file, needle, result);
            }
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("读取日志失败");
        }
        return result;
    }

    public static String formatSize(long size)
    {
        if (size < 1024)
        {
            return size + "B";
        }
        double value = size / 1024.0;
        if (value < 1024)
        {
            return String.format(Locale.ROOT, "%.2fKB", value);
        }
        value = value / 1024.0;
        if (value < 1024)
        {
            return String.format(Locale.ROOT, "%.2fMB", value);
        }
        return String.format(Locale.ROOT, "%.2fGB", value / 1024.0);
    }

    private boolean matchesKeyword(File file, LogFileItem item, String needle)
    {
        if (item.getFileName().toLowerCase(Locale.ROOT).contains(needle))
        {
            return true;
        }
        return contentContains(file, needle);
    }

    private boolean contentContains(File file, String needle)
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.toLowerCase(Locale.ROOT).contains(needle))
                {
                    return true;
                }
            }
            return false;
        }
        catch (IOException e)
        {
            return false;
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }

    private void fillTail(File file, LogFileContent result) throws IOException
    {
        long length = file.length();
        boolean truncated = length > TAIL_BYTES;
        int toRead = (int) Math.min(length, TAIL_BYTES);
        byte[] buf = new byte[toRead];
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try
        {
            raf.seek(length - toRead);
            raf.readFully(buf);
        }
        finally
        {
            raf.close();
        }
        String text = new String(buf, StandardCharsets.UTF_8);
        if (truncated)
        {
            int nl = text.indexOf('\n');
            if (nl >= 0)
            {
                text = text.substring(nl + 1);
            }
        }
        List<String> lines = splitLines(text);
        if (lines.size() > MAX_VIEW_LINES)
        {
            lines = lines.subList(lines.size() - MAX_VIEW_LINES, lines.size());
            truncated = true;
        }
        result.setTruncated(truncated);
        result.setLineCount(lines.size());
        result.setContent(join(lines));
    }

    private void fillMatched(File file, String keyword, LogFileContent result) throws IOException
    {
        String needle = keyword.toLowerCase(Locale.ROOT);
        Deque<String> matched = new ArrayDeque<String>();
        int total = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        try
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.toLowerCase(Locale.ROOT).contains(needle))
                {
                    total++;
                    matched.addLast(line);
                    if (matched.size() > MAX_VIEW_LINES)
                    {
                        matched.removeFirst();
                    }
                }
            }
        }
        finally
        {
            reader.close();
        }
        result.setTruncated(total > matched.size());
        result.setLineCount(matched.size());
        result.setContent(join(new ArrayList<String>(matched)));
    }

    private LogFileItem toItem(File file)
    {
        String key = file.getName();
        long length = file.length();
        long mtime = file.lastModified();
        Meta meta = cache.get(key);
        if (meta == null || meta.length != length || meta.mtime != mtime)
        {
            meta = readMeta(file, length, mtime);
            cache.put(key, meta);
        }
        LogFileItem item = new LogFileItem();
        item.setFileName(file.getName());
        item.setSize(length);
        item.setSizeText(formatSize(length));
        item.setStartTime(meta.start);
        item.setEndTime(meta.end);
        return item;
    }

    private Meta readMeta(File file, long length, long mtime)
    {
        String start = null;
        String end = null;
        try
        {
            start = firstTimestamp(file);
            end = lastTimestamp(file);
        }
        catch (IOException ignored)
        {
        }
        if (start == null)
        {
            Matcher matcher = NAME_DATE.matcher(file.getName());
            if (matcher.find())
            {
                start = matcher.group(1) + " 00:00:00";
            }
            else
            {
                start = formatTime(mtime);
            }
        }
        if (end == null)
        {
            end = formatTime(mtime);
        }
        return new Meta(length, mtime, start, end);
    }

    private String firstTimestamp(File file) throws IOException
    {
        int toRead = (int) Math.min(file.length(), HEAD_BYTES);
        if (toRead <= 0)
        {
            return null;
        }
        byte[] buf = new byte[toRead];
        FileInputStream in = new FileInputStream(file);
        try
        {
            int n = in.read(buf);
            if (n <= 0)
            {
                return null;
            }
            return findTimestamp(new String(buf, 0, n, StandardCharsets.UTF_8), true);
        }
        finally
        {
            in.close();
        }
    }

    private String lastTimestamp(File file) throws IOException
    {
        long length = file.length();
        if (length <= 0)
        {
            return null;
        }
        int toRead = (int) Math.min(length, TAIL_BYTES);
        byte[] buf = new byte[toRead];
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        try
        {
            raf.seek(length - toRead);
            raf.readFully(buf);
        }
        finally
        {
            raf.close();
        }
        String text = new String(buf, StandardCharsets.UTF_8);
        int nl = text.indexOf('\n');
        if (length > toRead && nl >= 0)
        {
            text = text.substring(nl + 1);
        }
        return findTimestamp(text, false);
    }

    static String findTimestamp(String text, boolean first)
    {
        if (text == null)
        {
            return null;
        }
        Matcher matcher = TIMESTAMP.matcher(text);
        String found = null;
        while (matcher.find())
        {
            found = matcher.group(1) + " " + matcher.group(2);
            if (first)
            {
                return found;
            }
        }
        return found;
    }

    private static boolean overlaps(LogFileItem item, String begin, String end)
    {
        String start = nullToEmpty(item.getStartTime());
        String finish = nullToEmpty(item.getEndTime());
        if (begin != null && finish.compareTo(begin) < 0)
        {
            return false;
        }
        if (end != null && start.compareTo(end) > 0)
        {
            return false;
        }
        return true;
    }

    private static String formatTime(long millis)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(SHANGHAI);
        return format.format(new Date(millis));
    }

    private static List<String> splitLines(String text)
    {
        List<String> lines = new ArrayList<String>();
        if (text == null || text.length() == 0)
        {
            return lines;
        }
        String[] parts = text.split("\\r?\\n", -1);
        for (String part : parts)
        {
            if (part.length() > 0)
            {
                lines.add(part);
            }
        }
        return lines;
    }

    private static String join(List<String> lines)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++)
        {
            if (i > 0)
            {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private static String blankToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    private static final class Meta
    {
        private final long length;
        private final long mtime;
        private final String start;
        private final String end;

        private Meta(long length, long mtime, String start, String end)
        {
            this.length = length;
            this.mtime = mtime;
            this.start = start;
            this.end = end;
        }
    }
}