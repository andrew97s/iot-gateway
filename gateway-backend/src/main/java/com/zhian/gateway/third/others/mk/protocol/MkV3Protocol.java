package com.zhian.gateway.third.others.mk.protocol;

import com.zhian.gateway.third.others.mk.constants.MkV3Type;
import com.zhian.gateway.third.others.mk.vo.MkV3Msg;
import com.zhian.gateway.third.others.mk.vo.MkV3Value;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 铭控 V3.3 标准协议编解码（帧头 A55A，帧尾 55AA，大端，CRC16-MODBUS）
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Slf4j
public final class MkV3Protocol {

    public static final int START = 0xA55A;
    public static final int END = 0x55AA;
    public static final int CMD_UPLOAD = 0x00;
    public static final int CMD_ACK = 0xFF;
    public static final int CMD_DEVICE_REPLY = 0x04;
    public static final int HEADER_LEN = 25;
    public static final int MIN_FRAME_LEN = 27;
    public static final int MAX_FRAME_LEN = 32 * 1024;

    private MkV3Protocol() {
    }

    public static boolean isV3Frame(byte[] bytes) {
        return bytes != null && bytes.length >= MIN_FRAME_LEN
                && ((bytes[0] & 0xFF) == 0xA5) && ((bytes[1] & 0xFF) == 0x5A)
                && ((bytes[bytes.length - 2] & 0xFF) == 0x55) && ((bytes[bytes.length - 1] & 0xFF) == 0xAA);
    }

    /**
     * @return 完整帧长度；-1 数据不足；-2 非法
     */
    public static int estimateFrameLength(ByteBuf in) {
        int idx = in.readerIndex();
        if (in.readableBytes() < 5) {
            return -1;
        }
        if ((in.getByte(idx) & 0xFF) != 0xA5 || (in.getByte(idx + 1) & 0xFF) != 0x5A) {
            return -2;
        }
        int frameLen = in.getUnsignedShort(idx + 3);
        if (frameLen < MIN_FRAME_LEN || frameLen > MAX_FRAME_LEN) {
            return -2;
        }
        if (in.readableBytes() < frameLen) {
            return -1;
        }
        if ((in.getByte(idx + frameLen - 2) & 0xFF) != 0x55 || (in.getByte(idx + frameLen - 1) & 0xFF) != 0xAA) {
            return -2;
        }
        return frameLen;
    }

    public static MkV3Msg parse(byte[] bytes) {
        if (!isV3Frame(bytes)) {
            return null;
        }
        int claimed = ((bytes[3] & 0xFF) << 8) | (bytes[4] & 0xFF);
        if (claimed != bytes.length) {
            log.error("铭控V3帧长度不一致, claimed={}, actual={}", claimed, bytes.length);
            return null;
        }
        int crcExpect = ((bytes[bytes.length - 4] & 0xFF) << 8) | (bytes[bytes.length - 3] & 0xFF);
        int crcActual = crc16Modbus(bytes, 0, bytes.length - 4);
        if (crcExpect != crcActual) {
            log.error("铭控V3 CRC校验失败, expect={}, actual={}", Integer.toHexString(crcExpect), Integer.toHexString(crcActual));
            return null;
        }

        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        try {
            // 帧头 A55A
            buf.skipBytes(2);
            int command = buf.readUnsignedByte();
            // 帧长度
            buf.skipBytes(2);
            MkV3Msg msg = new MkV3Msg();
            // 指令类型
            msg.setCommand(command);
            // 设备编码
            msg.setDeviceCode(readAscii(buf, 12));
            // 电量
            int battery = buf.readUnsignedByte();
            msg.setExternalPower((battery & 0x80) != 0);
            msg.setBatteryPercent((battery & 0x1F) * 5);
            // 信号强度
            msg.setRssi(buf.readByte());
            // 数据记录间隔
            msg.setIntervalSec(buf.readUnsignedShort());
            // 数据记录条数
            msg.setRecordCount(buf.readUnsignedShort());
            // 上传单位/表类型
            int deviceType = buf.readUnsignedByte();
            msg.setDeviceType(deviceType);
            // 设备报警状态
            msg.setAlarmStatus(buf.readUnsignedByte());
            // 监测类型
            MkV3Type type = MkV3Type.of(deviceType);
            if (type == null) {
                log.error("铭控V3未知设备类型: {}", deviceType);
                return null;
            }
            msg.setDeviceTypeName(type.getName());
            msg.setKind(type.getKind());

            int remainBeforeCrc = buf.readableBytes() - 4;
            int expected = type.getAlarmBytes() + type.getDataBytes() + 4 + msg.getRecordCount() * type.getDataBytes();
            if (remainBeforeCrc != expected) {
                log.error("铭控V3报文体长度不匹配, type={}, expect={}, actual={}, records={}",
                        deviceType, expected, remainBeforeCrc, msg.getRecordCount());
                return null;
            }

            // 阈值&当前监测值
            parseThresholds(buf, type, msg);
            List<MkV3Value> current = readRecord(buf, type);
            applyThresholds(current, msg);
            msg.setCurrentValues(current);
            // 记录时间
            msg.setTime(new Date(buf.readUnsignedInt() * 1000L));

            return msg;
        } catch (Exception e) {
            log.error("铭控V3协议解析失败,msg:{}", e.getMessage());
            return null;
        } finally {
            buf.release();
        }
    }

    public static byte[] buildAck(String deviceCode) {
        byte[] id = padDeviceCode(deviceCode);
        ByteBuf buf = Unpooled.buffer(MIN_FRAME_LEN);
        try {
            buf.writeShort(START);
            buf.writeByte(CMD_ACK);
            buf.writeShort(MIN_FRAME_LEN);
            buf.writeBytes(id);
            buf.writeInt((int) (System.currentTimeMillis() / 1000));
            buf.writeShort(0);
            byte[] prefix = new byte[buf.readableBytes()];
            buf.getBytes(0, prefix);
            buf.writeShort(crc16Modbus(prefix, 0, prefix.length));
            buf.writeShort(END);
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    static int crc16Modbus(byte[] data, int off, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= data[off + i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private static void parseThresholds(ByteBuf buf, MkV3Type type, MkV3Msg msg) {
        // 报警阈值根据报警长度变化
        int alarmBytes = type.getAlarmBytes();
        if (alarmBytes <= 0) {
            return;
        }
        ByteBuf alarmBuf = buf.readSlice(alarmBytes);
        List<String> lows = new ArrayList<>();
        List<String> highs = new ArrayList<>();
        for (int i = 0; i < type.getAlarmGroups(); i++) {
            MkV3Type.Field field = type.fieldAt(i);
            if (field == null || alarmBuf.readableBytes() < field.byteLength() * 2) {
                break;
            }
            lows.add(readNumber(alarmBuf, field));
            highs.add(readNumber(alarmBuf, field));
        }
        msg.setThresholdLows(lows);
        msg.setThresholdHighs(highs);
    }

    private static void applyThresholds(List<MkV3Value> values, MkV3Msg msg) {
        List<String> lows = msg.getThresholdLows();
        List<String> highs = msg.getThresholdHighs();
        if (values == null || lows == null) {
            return;
        }
        int n = Math.min(values.size(), Math.min(lows.size(), highs.size()));
        for (int i = 0; i < n; i++) {
            values.get(i).setThresholdLow(lows.get(i));
            values.get(i).setThresholdHigh(highs.get(i));
        }
    }

    private static List<MkV3Value> readRecord(ByteBuf buf, MkV3Type type) {
        List<MkV3Value> values = new ArrayList<>();
        int start = buf.readerIndex();
        List<MkV3Type.Field> fields = type.getFields();
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                MkV3Type.Field field = fields.get(i);
                if (buf.readableBytes() < field.byteLength()) {
                    break;
                }
                values.add(MkV3Value.builder()
                        .channel(i + 1)
                        .name(field.getName())
                        .unit(field.getUnit())
                        .monitorAlias(field.getMonitorAlias())
                        .value(readNumber(buf, field))
                        .build());
            }
        }
        int consumed = buf.readerIndex() - start;
        int remain = type.getDataBytes() - consumed;
        if (remain > 0 && buf.readableBytes() >= remain) {
            buf.skipBytes(remain);
        }
        return values;
    }

    private static String readNumber(ByteBuf buf, MkV3Type.Field field) {
        switch (field.getKind()) {
            case INT16_U:
                return scale(buf.readUnsignedShort(), field.getLsb());
            case INT32_S:
                return scale(buf.readInt(), field.getLsb());
            case INT32_U:
                return scale(buf.readUnsignedInt(), field.getLsb());
            case FLOAT32:
                return formatDecimal(buf.readFloat());
            case FLOAT64:
                return formatDecimal(buf.readDouble());
            case INT16_S:
            default:
                return scale(buf.readShort(), field.getLsb());
        }
    }

    private static String scale(long raw, double lsb) {
        if (Math.abs(lsb - 1) < 0.0000001) {
            return String.valueOf(raw);
        }
        return BigDecimal.valueOf(raw).multiply(BigDecimal.valueOf(lsb)).stripTrailingZeros().toPlainString();
    }

    private static String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String readAscii(ByteBuf buf, int len) {
        byte[] data = new byte[len];
        buf.readBytes(data);
        return new String(data, StandardCharsets.US_ASCII).trim();
    }

    private static byte[] padDeviceCode(String deviceCode) {
        byte[] id = new byte[12];
        byte[] src = deviceCode == null ? new byte[0] : deviceCode.getBytes(StandardCharsets.US_ASCII);
        int copy = Math.min(12, src.length);
        System.arraycopy(src, 0, id, 0, copy);
        return id;
    }
}
