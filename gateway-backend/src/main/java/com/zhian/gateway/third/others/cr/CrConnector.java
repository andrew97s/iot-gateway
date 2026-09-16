package com.zhian.gateway.third.others.cr;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 读卡器模块连接器
 *
 * @author tongwenjin
 * @since 2025 -4-24
 */
@Slf4j

public class CrConnector {

    /**
     * The constant serialPort.
     */
    private static SerialPort serialPort;
    /**
     * The constant readThread.
     */
    private static Thread readThread;
    /**
     * The Output stream.
     */
    private OutputStream outputStream;
    /**
     * The constant BLOCK_NO.
     */
    private static final byte BLOCK_NO = 0x01;

    /**
     * The address future.
     */
    private CompletableFuture<String> addressFuture = new CompletableFuture<>();
    /**
     * The Sn future.
     */
    private CompletableFuture<String> snFuture = new CompletableFuture<>();
    /**
     * The Data future.
     */
    private CompletableFuture<String> dataFuture = new CompletableFuture<>();
    /**
     * The Write future.
     */
    private CompletableFuture<Boolean> writeFuture = new CompletableFuture<>();

    /**
     * Connect port boolean.
     *
     * @param name the name
     * @return the boolean
     */
    public boolean connectPort(String name) {
        try {
            closePort();
            // 获取串口
            serialPort = SerialPort.getCommPort(name);
            if (!serialPort.openPort()) {
                log.error("串口打开失败!");
                return false;
            }

            // 配置串口参数
            serialPort.setBaudRate(9600);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

            // 获取输入流和输出流
            InputStream inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            // 启动读取线程
            readThread = new Thread(() -> readFromSerial(inputStream), name + "-reader");
            readThread.start();
        } catch (SerialPortInvalidPortException e) {
            log.info("串口连接失败,串口:{} 无效!", name);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Close port.
     */
    public void closePort() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            serialPort = null;
        }
        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
        }
    }

    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    /**
     * 读取串口数据
     *
     * @param inputStream the input stream
     */
    private void readFromSerial(InputStream inputStream) {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int len = inputStream.read(buffer);
                if (len > 0) {
                    StringBuilder hexString = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        hexString.append(String.format("%02X ", buffer[i]));
                    }

                    String cmd = hexString.toString().replace(" ", "");
                    log.debug("接收到数据: {}", hexString);
                    try {
                        processCmd(cmd);
                    }
                    catch (Exception e){
                        log.error("处理打卡器模块命令失败:{}" , e.getMessage());
                    }
                } else {
                    if (Thread.currentThread().isInterrupted()) {
                        // 线程中断
                        log.warn("read线程收到中断请求,退出处理。。。");
                        break;
                    }
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e) {
            // 忽略interrupt
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process cmd.
     *
     * @param resp the resp
     */
    private void processCmd(String resp) {
        // 命令
        String cmd = resp.substring(4, 6);
        // 结果
        String status = resp.substring(8, 10);

        switch (cmd) {
            // 读取卡片code
            case "A1": {
                if ("00".equals(status) && resp.length() == 24) {
                    String cardType = resp.substring(10, 14);
                    String cardCode = resp.substring(14, 22);
                    log.info("读取到卡片信息,cardType:{},cardCode:{}", cardType, cardCode);
                    snFuture.complete(cardCode);
                } else {
                    snFuture.complete("");
                    log.error("读取卡片code失败!");
                }
                break;
            }
            // 读取block数据
            case "A3": {
                if ("00".equals(status) && resp.length() == 44) {
                    String data = new String(hexStringToBytes(resp.substring(10, 40)), StandardCharsets.UTF_8);
                    log.info("读取到卡片block数据:{}", data);
                    dataFuture.complete(data);
                } else {
                    dataFuture.complete("");
                    log.error("读取卡片block数据失败!");
                }
                break;
            }
            // 写入block数据
            case "A4": {
                if ("00".equals(status) && resp.length() == 16) {
                    log.info("写入卡片block数据成功!");
                    writeFuture.complete(true);
                } else {
                    writeFuture.complete(false);
                    log.error("写入卡片block数据失败!");
                }
                break;
            }
            // 读写器地址
            case "B0": {
                if ("00".equals(status) && resp.length() == 16) {
                    log.info("读写器地址查询成功!");
                    addressFuture.complete(resp.substring(10,12));
                } else {
                    addressFuture.complete("-1");
                    log.error("读写器地址查询失败!");
                }
                break;
            }
            default:
                log.error("命令({})未知!", cmd);
        }
    }

    /**
     * 发送数据到串口
     *
     * @param outputStream the output stream
     * @param msg          the msg
     */
    public void sendToSerial(OutputStream outputStream, byte[] msg) {
        if (serialPort == null || !serialPort.isOpen()) {
            log.error("发送数据至串口失败,串口已关闭!");
            return;
        }
        // 校验和
        msg[msg.length - 1] = calculateChecksum(msg);

        try {
            outputStream.write(msg);
            log.debug("发送数据: {}", byteArrayToHex(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param byteArray the byte array
     * @return the string
     */
    public String byteArrayToHex(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString();
    }

    /**
     * 将十六进制字符串（无空格）转为字节数组
     *
     * @param hex the hex
     * @return the byte [ ]
     */
    public byte[] hexStringToBytes(String hex) {
        hex = hex.replaceAll("\\s", ""); // 移除空格
        hex = hex.replaceAll("00", ""); // 移除空格

        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        // 剔除0字节
        List<Byte> tempList = new ArrayList<>();
        for (byte b : bytes) {
            if (b != 0) {
                tempList.add(b);
            }
        }
        byte[] result = new byte[tempList.size()];
        for (int i = 0; i < tempList.size(); i++) {
            result[i] = tempList.get(i);
        }
        return result;
    }

    /**
     * 读取卡片编号
     *
     * @return the string
     */
    public String readCardCode() {
        // 给设备（串口）上电
        byte[] cmdBytes = {
                0x01, 0x08, (byte) 0xA1, 0x20, 0x00, 0x01, 0x00, 0x00
        };
        sendToSerial(outputStream, cmdBytes);
        try {
            String sn = snFuture.get(3, TimeUnit.SECONDS);
            snFuture = new CompletableFuture<>();
            return sn;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取读卡器地址
     *
     * @return the boolean
     */
    public String readAddress() {
        byte[] cmdBytes = {
                0x02, 0x08, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x45
        };
        sendToSerial(outputStream, cmdBytes);
        try {
            String address = addressFuture.get(100, TimeUnit.MILLISECONDS);
            addressFuture = new CompletableFuture<>();
            return address;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取卡片数据
     *
     * @return the string
     */
    public String readCardData() {
        // 读取2区数据
        byte[] cmdBytes = {
                0x01, 0x08, (byte) 0xA3, 0x20, BLOCK_NO, 0x00, 0x00, 0x00
        };
        sendToSerial(outputStream, cmdBytes);

        try {
            String sn = dataFuture.get(3, TimeUnit.SECONDS);
            dataFuture = new CompletableFuture<>();
            return sn;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 向卡片写入数据
     *
     * @param data the data
     * @return the string
     */
    public boolean writeCardData(String data) {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        if (dataBytes.length <= 15) {
            byte[] bytes = new byte[16];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = i < dataBytes.length ? dataBytes[i] : 0;
            }
            dataBytes = bytes;
        } else {
            throw new IllegalArgumentException("写入卡片数据长度必须小于15个字节!");
        }
        byte[] cmdBytes = new byte[23];
        cmdBytes[0] = 0x01;
        cmdBytes[1] = 0x17;
        cmdBytes[2] = (byte) 0xA4;
        cmdBytes[3] = 0x20;
        // 指定block 为 1
        cmdBytes[4] = BLOCK_NO;
        cmdBytes[5] = 0x01;
        // 数据位
        for (int i = 6; i < cmdBytes.length - 1; i++) {
            cmdBytes[i] = dataBytes[i - 6];
        }
        sendToSerial(outputStream, cmdBytes);
        try {
            boolean b = writeFuture.get(3, TimeUnit.SECONDS);
            writeFuture = new CompletableFuture<>();
            return b;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate checksum byte.
     *
     * @param data the data
     * @return the byte
     */
    private byte calculateChecksum(byte[] data) {
        byte checksum = data[0];
        for (int i = 1; i < data.length - 2; i++) {
            checksum ^= data[i];
        }
        // 取反
        return (byte) ~checksum;
    }
}
