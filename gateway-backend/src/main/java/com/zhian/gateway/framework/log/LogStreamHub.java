package com.zhian.gateway.framework.log;

import javax.websocket.Session;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时日志总线。业务线程只把文本放进队列，单独的线程再写到 WebSocket。
 */
public final class LogStreamHub
{
    private static final int RECENT_LIMIT = 800;

    private static final int SESSION_QUEUE = 2000;

    private static final Deque<String> recent = new ArrayDeque<String>();

    private static final ConcurrentHashMap<String, Holder> sessions = new ConcurrentHashMap<String, Holder>();

    private static final BlockingQueue<Integer> wake = new ArrayBlockingQueue<Integer>(1);

    private static final ThreadLocal<Boolean> reentry = new ThreadLocal<Boolean>();

    static
    {
        Thread worker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                drain();
            }
        }, "web-log-dispatch");
        worker.setDaemon(true);
        worker.start();
    }

    private LogStreamHub()
    {
    }

    public static void publish(String line)
    {
        if (line == null || line.length() == 0 || Boolean.TRUE.equals(reentry.get()))
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
        synchronized (recent)
        {
            for (String line : recent)
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
