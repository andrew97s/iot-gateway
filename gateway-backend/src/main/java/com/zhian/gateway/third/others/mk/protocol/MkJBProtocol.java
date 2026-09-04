package com.zhian.gateway.third.others.mk.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import com.zhian.gateway.third.others.mk.vo.MkJBMsg;

import java.nio.charset.StandardCharsets;

import static com.zhian.gateway.third.others.mk.constants.MkConsts.JB_TYPE_ACK;

/**
 * 铭控青鸟定制协议编解码（大端，帧头 @@，帧尾 ##）
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Slf4j
public final class MkJBProtocol {

    public static final byte[] START = {'@', '@'};
    public static final byte[] END = {'#', '#'};

    static final int MAX_CODE_LEN = 128;

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
            msg.setDataType(dataType);
            msg.setCode(readAscii(buf, codeLen));

            if (dataType == JB_TYPE_ACK) {
                if (buf.readableBytes() != 10) {
                    log.error("铭控青鸟协议应答帧长度非法: {}", bytes.length);
                    return null;
                }
                msg.setTimestamp(buf.readLong());
                return msg;
            }

            // 高低限 可能不存在！
            msg.setThresholdHigh(String.valueOf(buf.readUnsignedShort()));
            msg.setThresholdLow(String.valueOf(buf.readUnsignedShort()));
            msg.setValue(String.valueOf(buf.readUnsignedShort()));
            msg.setTimestamp(buf.readLong());

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
