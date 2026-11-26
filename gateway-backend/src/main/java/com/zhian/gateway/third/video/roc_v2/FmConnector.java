package com.zhian.gateway.third.video.roc_v2;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.zhian.gateway.common.exception.base.BaseException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.zhian.gateway.framework.web.websocket.consts.WebsocketConstants.IGNORE_LOCAL_IP;

/**
 * 人脸模块连接器
 *
 * @author tongwenjin
 * @since 2025 -04-01
 */
@Slf4j
public class FmConnector {

    /**
     * 串口对象
     */
    private static SerialPort serialPort;
    /**
     * The constant readThread.
     */
    private static Thread readThread;
    /**
     * The Output stream.
     */
    private static OutputStream outputStream;

    /**
     * The Enroll user future.
     */
    private static CompletableFuture<Integer> enrollUserFuture = new CompletableFuture<>();
    private static CompletableFuture<String> singleUserFuture = new CompletableFuture<>();

    /**
     * The Scan user future.
     */
    private static volatile CompletableFuture<Integer> scanUserFuture = new CompletableFuture<>();

    /**
     * The Scanning.
     */
    private static volatile AtomicBoolean scanning = new AtomicBoolean(false);
    private static volatile ReentrantLock scanLock = new ReentrantLock(false);
    private static volatile ReentrantLock lock = new ReentrantLock(false);

    /**
     * The Total user future.
     */
    private static CompletableFuture<TotalUser> totalUserFuture = new CompletableFuture<>();
    /**
     * The Pic note future.
     */
    private static CompletableFuture<PicState> picNoteFuture = new CompletableFuture<>();
    /**
     * The Sn future.
     */
    private static CompletableFuture<String> snFuture = new CompletableFuture<>();
    private static CompletableFuture<String> delFuture = new CompletableFuture<>();

    /**
     * Connect port.
     *
     * @param name the name
     */
    public static void connectPort(String name) {
        try {
            closePort();
            // 获取串口
            serialPort = SerialPort.getCommPort(name);
            if (!serialPort.openPort()) {
                log.error("串口打开失败!");
                return;
            }

            // 配置串口参数
            serialPort.setBaudRate(115200);
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
        }
    }

    /**
     * Close port.
     */
    public static void closePort() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            serialPort = null;
        }
        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
        }
    }

    public static boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    /**
     * 读取串口数据
     *
     * @param inputStream the input stream
     */
    private static void readFromSerial(InputStream inputStream) {
        try {
            byte[] buffer = new byte[1024];
            String lastContent = "";
            while (true) {
                int len = inputStream.read(buffer);
                // TODO 可能存在高频循环
                log.debug(" loop ... ");
                if (len > 0) {
                    StringBuilder hexString = new StringBuilder();
                    for (int i = 0; i < len; i++) {
                        hexString.append(String.format("%02X ", buffer[i]));
                    }

                    String resp = hexString.toString().replace(" ", "");
                    log.debug("接收到数据: {}", hexString);
                    try {
                        lastContent = processResp(lastContent, resp);
                    } catch (Exception e) {
                        log.error("处理响应数据发生异常: {}", e.getMessage());
                        e.printStackTrace();
                        lastContent = "";
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
     * Process resp string.
     *
     * @param lastContent the last content
     * @param resp        the resp
     * @return the string
     */
    private static String processResp(String lastContent, String resp) {
        // 新的响应
        if (resp.startsWith("EFAA")) {
            int dataLength = resp.length() <= 10 ? 0 : Integer.parseInt(resp.substring(6, 10), 16);

            // 当前响应未完成
            if (resp.length() <= 10 || (dataLength + 6) * 2 > resp.length()) {
                lastContent += (resp);
                log.debug("当前响应命令未完成");
            }
            // 等长
            else if ((dataLength + 6) * 2 == resp.length()) {
                processCmd(resp);
                lastContent = "";
            }
            // 超长
            else if ((dataLength + 6) * 2 < resp.length()) {
                String cmd = resp.substring(0, (dataLength + 6) * 2);
                processCmd(cmd);
                resp = resp.substring((dataLength + 6) * 2);
                lastContent = processResp(lastContent, resp);
            }
        }
        // 当前命令非完整
        else {
            // 尝试完整上一条命令
            if (!lastContent.isEmpty()) {
                if (resp.contains("EFAA")) {

                    String wholeResp = lastContent + resp.substring(0, resp.indexOf("EFAA"));
                    int dataLength = Integer.parseInt(wholeResp.substring(6, 10), 16);
                    if ((dataLength + 6) * 2 == resp.length()) {
                        log.debug("命令已完整 : {}", wholeResp);
                        processCmd(resp);

                        // 处理多余的部分
                        resp = resp.substring(resp.indexOf("EFAA"));
                        lastContent = "";
                        lastContent = processResp(lastContent, resp);
                    }
                    // 抛弃当前无效命令
                    else {
                        lastContent = "";
                        resp = resp.substring(resp.indexOf("EFAA"));
                        lastContent = processResp(lastContent, resp);
                    }
                }
                // 不包含则等待下一个命令
                else {
                    lastContent += (resp);

                    if (lastContent.length() > 10) {
                        int dataLength = Integer.parseInt(lastContent.substring(6, 10), 16);
                        if ((dataLength + 6) * 2 == lastContent.length()) {
                            log.debug("命令已完整 : {} ", lastContent);
                            processCmd(lastContent);
                            lastContent = "";
                        }
                    }
                }
            }
            // 直接尝试截取EFAA 前面无效部分
            else if (resp.contains("EFAA")) {
                resp = resp.substring(resp.indexOf("EFAA"));
                lastContent = processResp("", resp);
            }
            // 不包含EFAA的
            else {
                lastContent = resp;
            }
        }

        return lastContent;
    }

    /**
     * Process cmd.
     *
     * @param resp the resp
     */
    private static void processCmd(String resp) {
        String cmd = resp.substring(4, 6);

        switch (cmd) {
            // REPLY
            case "00": {
                String msgId = resp.substring(10, 12);
                CmdEnum cmdEnum = CmdEnum.getByCode(msgId);
                int result = Integer.parseInt(resp.substring(12, 14), 16);

                // 成功
                if (result == StatusEnum.SUCCESS.status) {
                    log.info("执行: {} ({}) 成功 !", cmdEnum.getName(), resp);
                    switch (msgId) {
                        // reset
                        case "10": {
                            break;
                        }
                        // get_status
                        case "11": {
                            String moduleStatus = resp.substring(14, 16);
                            log.info("获取到模块状态:{}", moduleStatus);
                            break;
                        }
                        // verify
                        case "12": {
                            int userId = Integer.parseInt(resp.substring(16, 18), 16);
                            scanUserFuture.complete(userId);
                            log.info("成功读取到人脸,userId:{}", userId);
                            break;
                        }
                        // enroll
                        case "13": {
                            break;
                        }
                        case "93": {
                            String sn = resp.substring(14, 30);
                            snFuture.complete(sn);
                            break;
                        }
                        // 图片录入
                        case "F7": {
                            // 根据最有一位校验是否录入成功
                            String userIndex = resp.substring(resp.length() - 4, resp.length() - 2);
                            int userId = Integer.parseInt(userIndex, 16);
                            if (userId != 0) {
                                enrollUserFuture.complete(userId);
                            }
                            // 获取sequence状态 EFAA000006F70000440010A5
                            int sequence = Integer.parseInt(resp.substring(16, 18), 16);
                            picNoteFuture.complete(
                                    new PicState(sequence, true, "成功")
                            );

                            break;
                        }
                        // enroll_single
                        case "1D": {
                            log.info("录入人脸成功! , {}", resp);
                            singleUserFuture.complete(Integer.parseInt(resp.substring(14, 18), 16) + "");
                            break;
                        }
                        // 删除用户
                        case "20": {
                            delFuture.complete("OK");
                            break;
                        }
                        // get_user
                        case "22": {
                            int userId = Integer.parseInt(resp.substring(14, 18), 16);
                            int isAdmin = Integer.parseInt(resp.substring(82, 84), 16);
                            byte[] userNameBytes = new byte[32];
                            for (byte i : userNameBytes) {
                                userNameBytes[i] = (byte) Integer.parseInt(resp.substring(18 + i * 2, 20 + i * 2), 16);
                            }
                            String userName = new String(userNameBytes);
                            break;
                        }
                        // get_all_users
                        case "24": {
                            // 解析结果
                            int totalUserCount = Integer.parseInt(resp.substring(14, 16), 16);
                            List<String> userIds = new ArrayList<>(totalUserCount + 1);
                            for (int i = 0; i < totalUserCount; i++) {
                                int userId = Integer.parseInt(resp.substring(16 + i * 4, 20 + i * 4), 16);
                                userIds.add(userId + "");
                            }

                            TotalUser total = new TotalUser();
                            total.setTotalCount(totalUserCount);
                            total.setUserIds(userIds);

                            totalUserFuture.complete(total);

                            break;
                        }
                    }
                } else {
                    StatusEnum statusEnum = StatusEnum.getByCode(result);
                    String msg = statusEnum.getMsg();
                    log.error("执行: {}({}) 异常,msg: {}", cmdEnum.getName(), resp, msg);
                    if ("F7".equals(msgId)) {
                        enrollUserFuture.complete(null);
                        int sequence = Integer.parseInt(resp.substring(16, 18), 16);
                        picNoteFuture.complete(new PicState(sequence, false, msg));
                    }
                    if ("12".equals(msgId)) {
                        scanUserFuture.complete(-1);
                    }
                    if ("1D".equals(msgId)) {
                        singleUserFuture.complete(msg);
                    }
                    if ("20".equals(msgId)) {
                        delFuture.complete(msg);
                    }
                }
                break;
            }
            // NOTE
            case "01": {
                int status = Integer.parseInt(resp.substring(10, 12), 16);
                // 算法执行成功
                if (status == 1) {
                    int state = Integer.parseInt(resp.substring(12, 14), 16);
                    FaceStateEnum stateEnum = FaceStateEnum.getByCode(state + "");
                    log.info("人脸识别状态: {}", stateEnum.name);
                }
                // 未知错误
                else if (status == 2) {
                    log.error("模组发生未知错误...");
                }
                break;
            }
            // IMAGE
            case "02": {
                break;
            }
        }
    }

    /**
     * 发送数据到串口
     *
     * @param outputStream the output stream
     * @param msg          the msg
     */
    public static void sendToSerial(OutputStream outputStream, byte[] msg) {
        if (serialPort == null || !serialPort.isOpen()) {
            log.error("发送数据至串口失败,串口已关闭!");
            return;
        }

        byte[] bytes = new byte[msg.length + 3];
        byte checkByte = 0x00;

        for (int i = 0; i < msg.length; i++) {
            bytes[2 + i] = msg[i];
            checkByte = (byte) (checkByte ^ msg[i]);
        }
        // 前两个字符固定为 0xef 和 0xaa
        bytes[0] = (byte) 0xef;
        bytes[1] = (byte) 0xaa;
        // 最后一个字符为 OXR 校验和
        bytes[bytes.length - 1] = checkByte;

        try {
            outputStream.write(bytes);
            if (log.isDebugEnabled()) {
                log.debug("发送数据: {}", byteArrayToHex(bytes));
            }
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
    public static String byteArrayToHex(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString();
    }

    /**
     * 获取设备SN(等待3S)
     *
     * @return the sn
     */
    public static String getSn() {
        return getSn(3000L);
    }

    /**
     * Gets sn.
     *
     * @param awaitMills the await mills
     * @return the sn
     */
    public static String getSn(Long awaitMills) {
        byte[] cmdBytes = {
                (byte) 0x93, 0x00, 0x00
        };
        sendToSerial(outputStream, cmdBytes);

        try {
            String sn = snFuture.get(awaitMills, TimeUnit.MILLISECONDS);
            snFuture = new CompletableFuture<>();
            return sn;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets all user.
     *
     * @return the all user
     */
    public static TotalUser getAllUser() {
        byte[] cmdBytes = {
                0x24, 0x00, 0x00
        };
        sendToSerial(outputStream, cmdBytes);

        try {
            TotalUser totalUser = totalUserFuture.get(3, TimeUnit.SECONDS);
            totalUserFuture = new CompletableFuture<>();
            return totalUser;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询指定用户信息.
     *
     * @param bizId the user id
     */
    public static void getUserInfo(String bizId) {
        String id = UserFaceMappingUtil.loadByBizId(bizId);
        byte[] cmdBytes = {
                0x22, 0x00, 0x02, 0x00, (byte) Integer.parseInt(id)
        };
        sendToSerial(outputStream, cmdBytes);
    }

    /**
     * 删除所有用户
     */
    public static void delAllUser() {
        byte[] cmdBytes = {
                0x21, 0x00, 0x00
        };
        sendToSerial(outputStream, cmdBytes);
        UserFaceMappingUtil.clear();
    }

    /**
     * 删除指定用户.
     *
     * @param bizId the user id
     */
    public static void delUser(String bizId) {
        String id = UserFaceMappingUtil.loadByBizId(bizId);
        if (StrUtil.isBlank(id)) {
            log.error("删除人脸数据失败:{} 对应的userId未匹配!", bizId);
            return;
        }
//        delById(id);
    }

    public static boolean delById(String id) throws ExecutionException, InterruptedException, TimeoutException {
        if (!StrUtil.isNumeric(id)) {
            throw new IllegalArgumentException("操作失败,非法参数!");
        }
        log.info("删除人脸数据(id:{})", id);
        byte[] getAllUser = {
                0x20, 0x00, 0x02, 0x00, (byte) Integer.parseInt(id)
        };
        sendToSerial(outputStream, getAllUser);
        String del = delFuture.get(3, TimeUnit.SECONDS);
        delFuture = new CompletableFuture<>();
        log.info("删除人脸数据(id:{})成功!", id);

        if (StrUtil.equals(del, "OK")) {
            return true;
        } else {
            throw new BaseException("操作失败," + del);
        }
    }

    /**
     * Reset enroll.
     */
    private static void resetEnroll() {
        byte[] getAllUser = {
                0x23, 0x00, 0x00
        };
        sendToSerial(outputStream, getAllUser);
    }

    /**
     * 录入用户人脸图片
     * 此方法不支持高频调用，建议控制调用频率为10s每次，否则可能出现录入失败现象
     *
     * @param picUrl the pic url
     * @param bizId  the biz id
     * @return the integer
     */
    @SuppressWarnings("BusyWait")
    public static Integer enrollPic(String picUrl, String bizId) {
        byte[] fileBytes;
        try {
            fileBytes = downloadUrlToBytes(picUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 照片数据需要拆包循环发送 , 单包246byte
        if (fileBytes.length > 0) {
            log.info("即将发送大小为:{} byte 的照片至模块!", fileBytes.length);

            byte[] lengthBytes = ByteBuffer.allocate(4).putInt(fileBytes.length).array();
            byte[] startCmdBytes = new byte[]{
                    // cmd
                    (byte) 0xF7,
                    // data length
                    (byte) 0x00, 0x07,
                    // sequence
                    (byte) 0x00, 0x00,
                    // image length
                    (byte) 0x00, 0x00, 0x00, 0x00,
                    // image type
                    0x00
            };
            startCmdBytes[5] = lengthBytes[0];
            startCmdBytes[6] = lengthBytes[1];
            startCmdBytes[7] = lengthBytes[2];
            startCmdBytes[8] = lengthBytes[3];
            sendToSerial(outputStream, startCmdBytes);

            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            int count = 0;
            int startIndex = 0;
            int endIndex = 246;
            while (true) {
                byte[] subData = Arrays.copyOfRange(fileBytes, startIndex, endIndex);
                log.debug("发送第 {} 次,人脸照片分包数据!", ++count);

                // 构造byte data
                byte[] cmdBytes = new byte[5 + subData.length];
                // cmd
                cmdBytes[0] = (byte) 0xF7;
                // data length
                cmdBytes[1] = 0x00;
                cmdBytes[2] = (byte) (2 + subData.length);
                // sequence
                cmdBytes[3] = 0x00;
                cmdBytes[4] = (byte) count;
                for (int i = 0; i < subData.length; i++) {
                    cmdBytes[i + 5] = subData[i];
                }
                try {
                    PicState state = picNoteFuture.get(3, TimeUnit.SECONDS);
                    picNoteFuture = new CompletableFuture<>();
                    // 失败提前返回
                    if (state != null && !state.isSuccess()) {
                        return null;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sendToSerial(outputStream, cmdBytes);

                // 判断是否跳出循环
                if (endIndex >= fileBytes.length) {
                    break;
                }
                startIndex = endIndex;
                endIndex = Math.min(startIndex + 246, fileBytes.length);
            }
        } else {
            log.error("读取照片失败:{}", picUrl);
        }

        try {
            Integer userId = enrollUserFuture.get(10, TimeUnit.SECONDS);
            enrollUserFuture = new CompletableFuture<>();
            // UserFaceMappingUtil.saveUser(userId, bizId);
            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Download url to bytes byte [ ].
     *
     * @param urlStr the url str
     * @return the byte [ ]
     * @throws Exception the exception
     */
    private static byte[] downloadUrlToBytes(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * 人脸录入
     *
     * @param userId the user id
     */
    public static Integer enrollSingle(String userId) throws TimeoutException, ExecutionException, InterruptedException {
        // 构造32位用户名称字节
        byte[] userIdBytes = userId.getBytes(StandardCharsets.UTF_8);
        int userIdLength = userIdBytes.length;
        if (userIdLength < 32) {
            byte[] newUserIdBytes = new byte[32];

            for (int i = 0; i < 32; i++) {
                if (i < 32 - userIdLength) {
                    newUserIdBytes[i] = 0x00;
                } else {
                    newUserIdBytes[i] = userIdBytes[i - (32 - userIdLength)];
                }
            }
            userIdBytes = newUserIdBytes;
        }

        byte[] bytes = new byte[38];

        // 指令位
        bytes[0] = 0x1d;
        // data length 位
        bytes[1] = 0x00;
        bytes[2] = 0x23;
        // 是否管理员
        bytes[3] = 0x01;
        // 人脸方向
        bytes[36] = 0x00;
        // 超时时间s
        bytes[37] = 0x1e;


        // 赋值用户名称字节
        for (int i = 4; i < bytes.length - 2; i++) {
            bytes[i] = userIdBytes[i - 4];
        }

        // 发送数据
        sendToSerial(outputStream, bytes);

        String result = singleUserFuture.get(10, TimeUnit.SECONDS);
        singleUserFuture = new CompletableFuture<>();

        if (!StrUtil.isNumeric(result)) {
            throw new BaseException(result);
        }
        log.info("成功录入人脸信息:{}", result);
        return Integer.parseInt(result);
    }

    /**
     * 执行识别
     *
     * @return 用户对应的businessId string
     */
    public static String verify() throws ExecutionException, InterruptedException, TimeoutException {
        try {
//            byte[] cmdBytes = {
//                    0x12, 0x00, 0x02, 0x01, 0x0a
//            };
            byte[] cmdBytes = {
                    0x12, 0x00, 0x02, 0x00, 0x0a
            };
            scanUserFuture = new CompletableFuture<>();
            sendToSerial(outputStream, cmdBytes);
            Integer userId = scanUserFuture.get(10, TimeUnit.SECONDS);
            if (userId != null) {
                log.info("识别到人脸用户：{}", userId);
                return userId + "";
            }
            return null;
        } finally {
            scanUserFuture = new CompletableFuture<>();
            log.info("人脸识别结束! {}", scanning.get());
        }
    }

    public static <T> T runWithLock(Callable<T> callable) throws Exception {
        if (!lock.tryLock()) {
            throw new BaseException("操作失败,模块执行中!");
        }

        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 视频预览拉流，需要注意十分钟后设备自动断电（人脸模组存在不能长时间通电问题）
     *
     * @return the string
     */
    public static String fetchStream() {
        // 等待秒后返回
        ThreadUtil.sleep(100);
        // 启动gst推流进程
        RuntimeUtil.exec("/bin/bash", "-c", "sudo systemctl start gst");
        log.info("gst 启动成功!");

        String ip = "";
        LinkedHashSet<String> ips = NetUtil.localIpv4s();
        for (String ipp : ips) {
            // 非etho网卡 ， 需要忽略部分IP
            if (!ipp.equals("127.0.0.1") && !ipp.equals("localhost") && !ipp.equals(IGNORE_LOCAL_IP)) {
                ip = ipp;
                break;
            }
        }
        // 直接返回SRS 代理视频流
        return "http://" + ip + ":8083/live/stream.live.flv";
    }

    /**
     * 关闭流
     */
    public static void closeStream() {
        // 停止gst推流进程
        RuntimeUtil.exec("/bin/bash", "-c", "sudo systemctl stop gst");
    }
}
