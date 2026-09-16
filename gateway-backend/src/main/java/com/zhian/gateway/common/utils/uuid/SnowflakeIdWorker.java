package com.zhian.gateway.common.utils.uuid;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

/**
 * 雪花ID生成器 (twitter开源的 高效分布式ID生成器)
 * 相较于UUID（无序 ， 128bit） , 具有 有序（时间戳控制）、定长（long 64bit）、高效的特性
 * 结构构成 timestampId + datacenterId + workerId + sequenceId
 * 当前实现使用nodeId 取代 datacenterId + workerId
 *
 * @author tongwenjin
 * @since 2022 /7/21
 */
public class SnowflakeIdWorker {

    /**
     * long的符号位 ， 默认为 0
     */
    private static final int UNUSED_BITS = 1;

    /**
     * 时间戳占用 41 位 ， 所以最大支持 2的41次方-1 约等于 69 年
     */
    private static final int EPOCH_BITS = 41;

    /**
     * nodeId（当前实例Id）  默认占用10位，现改成3位，减少生成ID的长度
     */
    private static final int NODE_ID_BITS = 3;

    /**
     * 序号ID 默认占用12位，现改成5位，减少生成ID的长度
     */
    private static final int SEQUENCE_BITS = 5;

    /**
     * 最大nodeId 2的10次方 - 1
     */
    private static final int MAX_NODE_ID = (int)(Math.pow(2, NODE_ID_BITS) - 1);

    /**
     * 最大序号ID 为 2的12次方 - 1
     */
    private static final int MAX_SEQUENCE = (int)(Math.pow(2, SEQUENCE_BITS) - 1);

    /**
     * 当前起点时间戳(2000-01-01)
     */
    private static final long CUSTOM_EPOCH = 946656000000L;

    /**
     * 节点ID , 可以指定（需要手动保证节点之前不能重复） ， 默认通过机器MAC地址生成
     */
    private final int nodeId;

    /**
     * 记录上次的时间戳
     */
    private volatile long lastTimestamp = -1L;

    /**
     * 序列号
     */
    private volatile long sequence = 0L;

    private volatile static SnowflakeIdWorker singletonInstance;

    /**
     * 私有化构造方法
     *
     * @param nodeId the node id
     */
    private SnowflakeIdWorker(int nodeId) {
        if(nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, MAX_NODE_ID));
        }
        this.nodeId = nodeId;
    }

    /**
     * 使用默认nodeId获取实例
     *
     * @return 单例模式实例
     */
    public static SnowflakeIdWorker getInstance(){
        return getInstance(null);
    }

    /**
     * 自定义nodeId 获取实例
     *
     * @return 单例模式实例
     */
    public static SnowflakeIdWorker getInstance(Integer nodeId) {
        // 双检锁实现单例模式懒加载
        if (singletonInstance == null) {
            synchronized (SnowflakeIdWorker.class) {
                if (singletonInstance == null) {
                    singletonInstance = new SnowflakeIdWorker(nodeId != null ? nodeId : createNodeId());
                }
            }
        }

        return singletonInstance;
    }

    /**
     * 获取ID
     *
     * @return the id
     */
    public synchronized long nextId() {
        long currentTimestamp = timestamp();

        // 时间戳回滚 ， 系统异常 ！！！
        if(currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }

        // 当前时间戳一致 代表同一毫秒值内获取ID
        if (currentTimestamp == lastTimestamp) {
            // 序列号与最大序列号进行 与 运算 ，实现sequence循环
            sequence = (sequence + 1) & MAX_SEQUENCE;

            // 序号为0 代表序号已经消耗完 ， 需要阻塞到下一毫秒值
            if(sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        }
        // 当前时间戳不一致 初始化序列号0
        else {
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        return
                // 时间戳左移 22位
                currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS)
                // nodeId左移12位
                | ((long) nodeId << SEQUENCE_BITS)
                // 序列号占10位
                | sequence;
    }

    public String nextStringId() {
        return String.valueOf(nextId());
    }


    /**
     * 获取当前时间戳与 自定义初始毫秒值 {@link CUSTOM_EPOCH} 的差值 构成ID的时间戳
     * 此处默认时间戳为2000 年 ， 故此处获取的时间戳为 currentTime - (2000-1970) 的时间戳
     * 代表最大支持未来 2039 - currentYear(2022) + （2000-1970） = 47 年内的 ID
     *
     * @return 获取到的时间戳
     */
    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;
    }

    /**
     * 循环获取时间戳，下一毫秒值
     *
     * @param currentTimestamp 当前时间戳
     * @return 获取到新的时间戳
     */
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    /**
     * 获取本机的MAC地址生成对应nodeId
     *
     * @return nodeId
     */
    private static int createNodeId() {
        int nodeId;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                }
            }
            nodeId = sb.toString().hashCode();
        } catch (Exception ex) {
            nodeId = (new SecureRandom().nextInt());
        }
        nodeId = nodeId & MAX_NODE_ID;
        return nodeId;
    }

    /**
     * 测试
     * @param args
     */
    public static void main(String[] args){
        System.out.println("ts: " + System.currentTimeMillis());
        for(int i=0;i<100;i++){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("id: " + getInstance().nextId());
        }
    }
}
