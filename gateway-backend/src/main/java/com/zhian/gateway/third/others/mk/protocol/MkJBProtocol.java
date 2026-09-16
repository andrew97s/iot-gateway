package com.zhian.gateway.third.others.mk.protocol;

import com.zhian.gateway.third.others.mk.vo.MkJBMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.zhian.gateway.third.others.mk.constants.MkConsts.JB_TYPE_ACK;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.JB_TYPE_CHANGE_CM;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.JB_TYPE_PERIOD_CM;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.KIND_LEVEL;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.KIND_PRESSURE;
import static com.zhian.gateway.third.others.mk.constants.MkConsts.KIND_PUMP;

/**
 * 铭控青鸟定制协议编解码（大端，帧头 @@，帧尾 ##）。
 * <p>
 * 三水三类设备共用帧头，载荷不同：
 * <ul>
 *   <li>压力：上限 + 下限 + 当前值（kPa）+ 时间 + ICCID</li>
 *   <li>液位：上限 + 下限 + 当前值（阈值毫米；类型 5/6 当前值为厘米）+ 时间 + ICCID</li>
 *   <li>水泵：当前状态（0 关 / 1 开）+ 时间 + ICCID，无高低限</li>
 * </ul>
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Slf4j
public final class MkJBProtocol {

    public static final byte[] START = {'@', '@'};
    public static final byte[] END = {'#', '#'};

    static final int MAX_CODE_LEN = 128;
    static final int MAX_ICCID_LEN = 64;

    private MkJBProtocol() {
    }

    public static boolean isJbFrame(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && bytes[0] == START[0] && bytes[1] == START[1]
                && bytes[bytes.length - 2] == END[0] && bytes[bytes.length - 1] == END[1];
    }

    public static MkJBMsg parse(byte[] bytes) {
        if (!isJbFrame(bytes)) {
            return null;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        try {
            buf.skipBytes(2);
            int dataType = buf.readUnsignedShort();
            int codeLen = buf.readUnsignedShort();
            if (codeLen < 0 || codeLen > MAX_CODE_LEN || buf.readableBytes() < codeLen + 2) {
                log.error("铭控青鸟协议设备编号长度非法: {}", codeLen);
                return null;
            }
            MkJBMsg msg = new MkJBMsg();
            // 数据类型
            msg.setDataType(dataType);
            // 设备编码
            msg.setCode(readAscii(buf, codeLen));
            // 校验可读数据长度
            if (dataType == JB_TYPE_ACK) {
                if (buf.readableBytes() < 10) {
                    log.error("铭控青鸟协议应答帧长度非法: {}", bytes.length);
                    return null;
                }
                msg.setTimestamp(buf.readLong());
                skipEnd(buf);
                return msg;
            }
            int remain = buf.readableBytes() - 2;
            if (remain < 8) {
                log.error("铭控青鸟协议报文体过短, remain={}", remain);
                return null;
            }
            // 数据解析
            if (isPumpPayload(buf, remain)) {
                parsePump(buf, msg);
            }
            else {
                parseAnalog(buf, msg);
            }
            skipEnd(buf);
            return msg;
        } catch (Exception e) {
            log.error("铭控青鸟协议解析失败,msg:{}", e.getMessage());
            return null;
        } finally {
            buf.release();
        }
    }

    public static byte[] buildAck(MkJBMsg msg) {
        byte[] code = toAscii(msg.getCode());
        ByteBuf buf = Unpooled.buffer(6 + code.length + 10);
        try {
            buf.writeBytes(START);
            buf.writeShort(JB_TYPE_ACK);
            buf.writeShort(code.length);
            buf.writeBytes(code);
            buf.writeLong(msg.getTimestamp() > 0 ? msg.getTimestamp() : System.currentTimeMillis());
            buf.writeBytes(END);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    /**
     * 下发阈值：type=1 上限 / 2 下限 / 3 全部
     */
    public static byte[] buildSetCmd(String deviceCode, int cmdType, int high, int low) {
        byte[] code = toAscii(deviceCode);
        ByteBuf buf = Unpooled.buffer(6 + code.length + 14);
        try {
            buf.writeBytes(START);
            buf.writeShort(cmdType);
            buf.writeShort(code.length);
            buf.writeBytes(code);
            buf.writeShort(high);
            buf.writeShort(low);
            buf.writeLong(System.currentTimeMillis());
            buf.writeBytes(END);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    /**
     * 水泵：状态2 + 时间8 [+ ICCID长度2 + ICCID n] = 10 或 12+n。
     * 压力/液位：上限2 + 下限2 + 当前值2 + 时间8 [+ ICCID] = 14 或 16+n。
     */
    private static boolean isPumpPayload(ByteBuf buf, int remain) {
        int idx = buf.readerIndex();
        if (remain == 10) {
            return true;
        }
        if (remain == 14) {
            return false;
        }
        if (remain >= 16) {
            int analogIccid = buf.getUnsignedShort(idx + 14);
            if (iccidLenOk(analogIccid) && 16 + analogIccid == remain) {
                return false;
            }
        }
        if (remain >= 12) {
            int pumpIccid = buf.getUnsignedShort(idx + 10);
            if (iccidLenOk(pumpIccid) && 12 + pumpIccid == remain) {
                return true;
            }
        }
        return remain < 14;
    }

    private static boolean iccidLenOk(int len) {
        return len >= 0 && len <= MAX_ICCID_LEN;
    }


    private static void parsePump(ByteBuf buf, MkJBMsg msg) {
        msg.setKind(KIND_PUMP);
        msg.setUnit("status");
        msg.setValue(String.valueOf(buf.readUnsignedShort()));
        msg.setTimestamp(buf.readLong());
    }

    private static void parseAnalog(ByteBuf buf, MkJBMsg msg) {
        int dataType = msg.getDataType();
        boolean levelCm = dataType == JB_TYPE_PERIOD_CM || dataType == JB_TYPE_CHANGE_CM;
        msg.setKind(levelCm ? KIND_LEVEL : KIND_PRESSURE);
        msg.setUnit(levelCm ? "cm" : "kPa");
        msg.setThresholdHigh(String.valueOf(buf.readUnsignedShort()));
        msg.setThresholdLow(String.valueOf(buf.readUnsignedShort()));
        msg.setValue(String.valueOf(buf.readUnsignedShort()));
        msg.setTimestamp(buf.readLong());
    }

    private static void skipEnd(ByteBuf buf) {
        if (buf.readableBytes() >= 2) {
            buf.skipBytes(2);
        }
    }

    private static String readAscii(ByteBuf buf, int len) {
        if (len <= 0) {
            return "";
        }
        byte[] data = new byte[len];
        buf.readBytes(data);
        return new String(data, StandardCharsets.UTF_8).trim();
    }

    private static byte[] toAscii(String text) {
        if (text == null) {
            return new byte[0];
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
