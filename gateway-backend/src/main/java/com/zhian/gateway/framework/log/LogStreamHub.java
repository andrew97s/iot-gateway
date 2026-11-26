package com.zhian.gateway.framework.log;

import javax.websocket.Session;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时日志总线。业务线程只把文本放进队列，单独的线程再写到 WebSocket。
 */
public final class LogStreamHub
{
    public static final int RECENT_LIMIT = 800;

    private static final int SESSION_QUEUE = 2000;

    private static final Deque<String> recent = new ArrayDeque<String>();

    private static final ConcurrentHashMap<String, Holder> sessions = new ConcurrentHashMap<String, Holder>();

    private static final BlockingQueue<Integer> wake = new ArrayBlockingQueue<Integer>(1);

    private static final ThreadLocal<Boolean> reentry = new ThreadLocal<Boolean>();

    private static final ThreadLocal<Boolean> dispatching = new ThreadLocal<Boolean>();

    private static volatile Snapshot snapshot;

    /**
     * 连接建立时回放当前日志文件末尾。由日志服务在启动后注册。
     */
    public interface Snapshot
    {
        List<String> lines();
    }

    static
    {
        Thread worker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                dispatching.set(Boolean.TRUE);
                drain();
            }
        }, "web-log-dispatch");
        worker.setDaemon(true);
        worker.start();
    }

    private LogStreamHub()
    {
    }

    public static void setSnapshot(Snapshot snapshot)
    {
        LogStreamHub.snapshot = snapshot;
    }

    public static void publish(String line)
    {
        if (line == null || line.length() == 0 || Boolean.TRUE.equals(reentry.get()) || Boolean.TRUE.equals(dispatching.get()))
        {
            return;
        }
        reentry.set(Boolean.TRUE);
        try
        {
            String text = stripNewline(line);
            if (text.length() == 0)
            {
                return;
            }
            synchronized (recent)
            {
                recent.addLast(text);
                while (recent.size() > RECENT_LIMIT)
                {
                    recent.removeFirst();
                }
                for (Holder holder : sessions.values())
                {
                    offer(holder, text);
                }
            }
            wake.offer(1);
        }
        finally
        {
            reentry.remove();
        }
    }

    public static void register(String id, Session session)
    {
        Holder holder = new Holder(session);
        List<String> seed = readSnapshot();
        synchronized (recent)
        {
            if (seed.isEmpty())
            {
                seed = new ArrayList<String>(recent);
            }
            for (String line : seed)
            {
                offer(holder, line);
            }
            sessions.put(id, holder);
        }
        wake.offer(1);
    }

    public static void unregister(String id)
    {
        if (id != null)
        {
            sessions.remove(id);
        }
    }

    public static int recentSize()
    {
        synchronized (recent)
        {
            return recent.size();
        }
    }

    private static void drain()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                wake.take();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
            boolean pending = true;
            while (pending)
            {
                pending = false;
                for (Map.Entry<String, Holder> entry : sessions.entrySet())
                {
                    Holder holder = entry.getValue();
                    if (holder.session == null || !holder.session.isOpen())
                    {
                        sessions.remove(entry.getKey());
                        continue;
                    }
                    String line = holder.queue.poll();
                    if (line == null)
                    {
                        continue;
                    }
                    pending = true;
                    send(holder.session, line);
                }
            }
        }
    }

    private static List<String> readSnapshot()
    {
        Snapshot current = snapshot;
        if (current == null)
        {
            return new ArrayList<String>();
        }
        try
        {
            List<String> lines = current.lines();
            if (lines == null || lines.isEmpty())
            {
                return new ArrayList<String>();
            }
            if (lines.size() <= RECENT_LIMIT)
            {
                return lines;
            }
            return new ArrayList<String>(lines.subList(lines.size() - RECENT_LIMIT, lines.size()));
        }
        catch (Exception e)
        {
            return new ArrayList<String>();
        }
    }

    private static void offer(Holder holder, String text)
    {
        if (!holder.queue.offer(text))
        {
            holder.queue.poll();
            holder.queue.offer(text);
        }
    }

    private static void send(Session session, String line)
    {
        try
        {
            synchronized (session)
            {
                if (session.isOpen())
                {
                    session.getBasicRemote().sendText(line);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static String stripNewline(String line)
    {
        int end = line.length();
        while (end > 0)
        {
            char ch = line.charAt(end - 1);
            if (ch != '\n' && ch != '\r')
            {
                break;
            }
            end--;
        }
        return line.substring(0, end);
    }

    private static final class Holder
    {
        private final Session session;
        private final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(SESSION_QUEUE);

        private Holder(Session session)
        {
            this.session = session;
        }
    }
}