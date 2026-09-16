package com.zhian.gateway.third.utils;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.zhian.gateway.third.others.cr.CrConnector;
import com.zhian.gateway.third.video.roc_v2.FmConnector;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 串口工具类
 *
 * @author tongwenjin
 * @since 2025 -4-29
 */
@Slf4j
public class SerialPortUtil {

    /**
     * The constant lastOpTime.
     */
    private static long lastOpTime = System.currentTimeMillis();

    /**
     * The constant DEVICE_URI.
     */
    private static final String DEVICE_URI = "/sys/class/gpio_innohi/ext_uart_pwr/value";

    /**
     * The constant crDeviceName.
     */
    private static String crDeviceName;

    /**
     * The constant fmDeviceName.
     */
    private static String fmDeviceName;

    private static boolean DEVICE_URI_EXISTS = false;

    static {
        File file = new File(DEVICE_URI);
        if (file.exists()) {
            DEVICE_URI_EXISTS = true;
        } else {
            log.error("当前 UART_PWR 不存在!");
        }
    }

    /**
     * The constant scheduler.
     */
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("SP-PowerOff-Scheduler");
        return t;
    });

    // 初始化时启动
    static {
        // 每10秒检查一次
        scheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastOpTime > 180000 && "1".equals(getPowerStatus())) {
                log.info("串口操作超时,执行断电操作...");
                powerOff();
                lastOpTime = System.currentTimeMillis();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * 串口上电
     */
    public static void powerOn() {
        powerOn(1200);
    }

    /**
     * Power on no delay.
     */
    public static void powerOnNoDelay() {
        powerOn(100);
    }

    /**
     * 串口上电
     *
     * @param awaitMills the await mills
     */
    public static void powerOn(long awaitMills) {

        if (!DEVICE_URI_EXISTS) {
            return;
        }

        lastOpTime = System.currentTimeMillis();
        if ("0".equals(getPowerStatus())) {
            try {
                Files.write(Paths.get(DEVICE_URI), "1".getBytes(), StandardOpenOption.WRITE);
                log.info("串口上电成功!");
            } catch (IOException e) {
                log.error("串口上电操作失败:{}", e.getMessage());
            }
            // 上电成功后一般至少需要等待1-2s 设备才会就绪，此处等待1.2s
            ThreadUtil.sleep(awaitMills);
        }
    }

    /**
     * 串口断电
     */
    public static void powerOff() {

        if (!DEVICE_URI_EXISTS) {
            return;
        }

        if ("1".equals(getPowerStatus())) {
            try {
                FmConnector.closeStream();
                Files.write(Paths.get(DEVICE_URI), "0".getBytes(), StandardOpenOption.WRITE);
                log.info("串口断电成功!");
            } catch (IOException e) {
                log.error("串口断电操作失败:{}", e.getMessage());
            }
        }
    }

    public static void powerOff(Long delayMills) {
        if (delayMills > 0) {
            CompletableFuture.runAsync(() -> {
                ThreadUtil.sleep(delayMills);
                powerOff();
            });
        }
        else {
            powerOff();
        }
    }

    /**
     * Gets power status.
     *
     * @return the power status
     */
    private static String getPowerStatus() {
        if (!DEVICE_URI_EXISTS) {
            return "0";
        }

        try (BufferedReader br = new BufferedReader(new FileReader(DEVICE_URI))) {
            String line = br.readLine();
            return line.trim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 查询读卡器模块串口地址
     *
     * @return the string
     */
    public static String searchCrDevice() {
        if (StrUtil.isNotBlank(crDeviceName)) {
            return crDeviceName;
        }
        try {
            powerOn();
            for (int i = 0; i < 10; i++) {
                String name = "/dev/ttyS" + i;

                String address = "";
                CrConnector connector = new CrConnector();
                try {
                    connector.connectPort(name);
                    address = connector.readAddress();
                }
                // 忽略异常
                catch (Exception e) {
                    continue;
                } finally {
                    connector.closePort();
                }

                // 为空代表读取数据失败
                if (StrUtil.isNotBlank(address)) {
                    crDeviceName = name;
                    log.info("搜索读卡器模块串口地址成功: {} !", name);
                    return name;
                }
            }

            log.error("搜索读卡器串口地址失败!");
        } finally {
            powerOff();
        }
        return "";
    }

    /**
     * 查询人脸模块串口地址
     *
     * @return the string
     */
    public static String searchFmDevice() {
        if (StrUtil.isNotBlank(fmDeviceName)) {
            return fmDeviceName;
        }
        try {
            powerOn();
            for (int i = 0; i < 10; i++) {
                String name = "/dev/ttyS" + i;
                String code = "";
                try {
                    FmConnector.connectPort(name);
                    code = FmConnector.getSn(200L);
                } catch (Exception e) {
                    continue;
                } finally {
                    FmConnector.closePort();
                }
                if (StrUtil.isNotBlank(code)) {
                    fmDeviceName = name;
                    log.info("搜索人脸模块串口地址成功: {} !", fmDeviceName);
                    return name;
                }
            }

            log.error("搜索人脸模块串口地址失败!");
        } finally {
            powerOff();
        }
        return "";
    }
}
