package com.zhian.gateway.third.others.mklora.protocol;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import com.zhian.gateway.third.others.mklora.constants.MkLoraConsts;
import com.zhian.gateway.third.others.mklora.vo.MkLoraReportMsg;

import java.util.Arrays;
import java.util.Date;

/**
 * 铭控 LoRa 智能无线终端通讯协议（V1.8）编解码。
 *
 * <p>仅实现主动上报（命令 0x10）解析。数据为大端；CRC16 采用 Modbus
 * （多项式 0x8005 / 反射 0xA001，初值 0xFFFF），校验码在帧中按大端存放。</p>
 */
public final class MkLoraProtocol {

    private MkLoraProtocol() {
    }

    /**
     * 解析完整主动上报帧。
     */
    public static MkLoraReportMsg decodeReport(byte[] frame) {
        if (frame == null || frame.length < MkLoraConsts.MIN_FRAME_LENGTH) {
            throw new IllegalArgumentException("上报报文长度不足");
        }
        int header = unsignedShort(frame[0], frame[1]);
        if (header != MkLoraConsts.FRAME_HEADER) {
            throw new IllegalArgumentException("帧头错误: 0x" + Integer.toHexString(header));
        }
        int footer = unsignedShort(frame[frame.length - 2], frame[frame.length - 1]);
        if (footer != MkLoraConsts.FRAME_FOOTER) {
            throw new IllegalArgumentException("帧尾错误: 0x" + Integer.toHexString(footer));
        }

        int cmd = unsigned(frame[2]);
        if (cmd != MkLoraConsts.CMD_REPORT) {
            throw new IllegalArgumentException("非主动上报命令: 0x" + Integer.toHexString(cmd));
        }

        int declaredLength = unsigned(frame[3]);
        if (declaredLength != frame.length) {
            throw new IllegalArgumentException(
                    "帧长度不匹配, declared=" + declaredLength + ", actual=" + frame.length
            );
        }
        if (declaredLength > MkLoraConsts.MAX_FRAME_LENGTH) {
            throw new IllegalArgumentException("帧长度超限: " + declaredLength);
        }

        int expectedCrc = unsignedShort(frame[frame.length - 4], frame[frame.length - 3]);
        int actualCrc = crc16(frame, 0, frame.length - 4);
        if (expectedCrc != actualCrc) {
            throw new IllegalArgumentException(
                    "CRC 校验失败, expected=0x" + Integer.toHexString(expectedCrc)
                            + ", actual=0x" + Integer.toHexString(actualCrc)
            );
        }

        long deviceId = unsignedInt(frame[4], frame[5], frame[6], frame[7]);
        int batteryRaw = unsigned(frame[8]);
        int signalRaw = frame[9]; // signed
        int sensorType = unsigned(frame[10]);
        int alarmRaw = unsigned(frame[11]);

        int dataLength = declaredLength - MkLoraConsts.FRAME_OVERHEAD;
        byte[] data = dataLength <= 0
                ? new byte[0]
                : Arrays.copyOfRange(frame, 12, 12 + dataLength);

        boolean lowAlarm = bit(alarmRaw, 0);
        boolean highAlarm = bit(alarmRaw, 1);
        boolean batteryAlarm = bit(alarmRaw, 2);
        boolean sensorFault = bit(alarmRaw, 3);
        boolean hardwareFault = bit(alarmRaw, 4);
        boolean sensor2LowAlarm = bit(alarmRaw, 5);
        boolean sensor2HighAlarm = bit(alarmRaw, 6);
        boolean sensor3Alarm = bit(alarmRaw, 7);

        return MkLoraReportMsg.builder()
                .deviceId(deviceId)
                .deviceCode(formatDeviceCode(deviceId))
                .batteryRaw(batteryRaw)
                .externalPower(bit(batteryRaw, 7))
                .supplyMode((batteryRaw >> 5) & 0x03)
                .batteryPercent((batteryRaw & 0x1F) * 5)
                .signalDbm(signalRaw)
                .sensorType(sensorType)
                .sensorTypeName(MkLoraSensorTypes.typeName(sensorType))
                .alarmRaw(alarmRaw)
                .lowAlarm(lowAlarm)
                .highAlarm(highAlarm)
                .batteryAlarm(batteryAlarm)
                .sensorFault(sensorFault)
                .hardwareFault(hardwareFault)
                .sensor2LowAlarm(sensor2LowAlarm)
                .sensor2HighAlarm(sensor2HighAlarm)
                .sensor3Alarm(sensor3Alarm)
                .alarmed(alarmRaw != 0)
                .values(MkLoraSensorTypes.parseValues(sensorType, data))
                .dataHex(toHex(data))
                .rawHex(toHex(frame))
                .time(new Date())
                .build();
    }

    public static int crc16(byte[] data, int offset, int length) {
        CRC16Modbus crc = new CRC16Modbus();
        crc.update(data, offset, length);
        return (int) (crc.getValue() & 0xFFFF);
    }

    public static String formatDeviceCode(long deviceId) {
        long plate = deviceId % 100_000_000L;
        return String.format("%08d", plate);
    }

    public static int unsigned(byte value) {
        return value & 0xFF;
    }

    public static int unsignedShort(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    public static long unsignedInt(byte b0, byte b1, byte b2, byte b3) {
        return ((long) (b0 & 0xFF) << 24)
                | ((long) (b1 & 0xFF) << 16)
                | ((long) (b2 & 0xFF) << 8)
                | (long) (b3 & 0xFF);
    }

    private static boolean bit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
