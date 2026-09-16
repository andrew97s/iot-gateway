package com.zhian.gateway.third.others.mklora.protocol;

import com.zhian.gateway.third.others.mklora.constants.MkLoraConsts;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 铭控 LoRa TCP 粘包/拆包解码器，仅产出主动上报帧解析结果。
 */
@Slf4j
public class MkLoraFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        // 先打印本次缓冲区可读的全部原始字节，便于对照发送端
        log.info("铭控LoRa TCP收到原始数据 len={}, HEX={}", in.readableBytes(), dumpReadableHex(in));

        while (in.isReadable()) {
            int frameStart = findHeader(in);
            if (frameStart < 0) {
                log.warn("铭控LoRa未找到帧头A55A, 剩余可读={}字节, HEX={}",
                        in.readableBytes(), dumpReadableHex(in));
                return;
            }
            if (in.writerIndex() - frameStart < 4) {
                in.readerIndex(frameStart);
                log.info("铭控LoRa帧头已找到但数据不足4字节, 等待后续分包, 当前HEX={}", dumpReadableHex(in));
                return;
            }

            int cmd = in.getUnsignedByte(frameStart + 2);
            int frameLength = in.getUnsignedByte(frameStart + 3);
            if (frameLength < MkLoraConsts.MIN_FRAME_LENGTH
                    || frameLength > MkLoraConsts.MAX_FRAME_LENGTH) {
                log.warn("铭控LoRa帧长度非法: {}, 丢弃帧头, 当前HEX={}", frameLength, dumpReadableHex(in));
                in.readerIndex(frameStart + 2);
                continue;
            }
            if (in.writerIndex() - frameStart < frameLength) {
                in.readerIndex(frameStart);
                log.info("铭控LoRa帧未收齐, 需要{}字节, 当前{}字节, HEX={}",
                        frameLength, in.readableBytes(), dumpReadableHex(in));
                return;
            }

            int footer = in.getUnsignedShort(frameStart + frameLength - 2);
            if (footer != MkLoraConsts.FRAME_FOOTER) {
                log.warn("铭控LoRa帧尾非法: 0x{}, 丢弃帧头", Integer.toHexString(footer));
                in.readerIndex(frameStart + 2);
                continue;
            }

            byte[] frame = new byte[frameLength];
            in.readerIndex(frameStart);
            in.readBytes(frame);
            log.info("铭控LoRa完整帧(HEX): {}", MkLoraProtocol.toHex(frame));

            if (cmd != MkLoraConsts.CMD_REPORT) {
                log.info("忽略非上报命令帧: cmd=0x{}, HEX={}", Integer.toHexString(cmd), MkLoraProtocol.toHex(frame));
                continue;
            }
            try {
                out.add(MkLoraProtocol.decodeReport(frame));
            } catch (IllegalArgumentException e) {
                log.warn("铭控LoRa上报解析失败: {}, raw={}", e.getMessage(), MkLoraProtocol.toHex(frame));
            }
        }
    }

    /**
     * 不移动 readerIndex，仅导出当前可读区间十六进制。
     */
    private static String dumpReadableHex(ByteBuf in) {
        int readerIndex = in.readerIndex();
        int len = in.readableBytes();
        if (len <= 0) {
            return "";
        }
        byte[] bytes = new byte[len];
        in.getBytes(readerIndex, bytes);
        return MkLoraProtocol.toHex(bytes);
    }

    private int findHeader(ByteBuf in) {
        while (in.readableBytes() >= 2) {
            int index = in.readerIndex();
            int header = in.readUnsignedShort();
            if (header == MkLoraConsts.FRAME_HEADER) {
                in.readerIndex(index);
                return index;
            }
            in.readerIndex(index + 1);
        }
        if (in.isReadable()) {
            // 保留可能的半个帧头
            int b = in.getUnsignedByte(in.readerIndex());
            if (b != ((MkLoraConsts.FRAME_HEADER >> 8) & 0xFF)) {
                in.skipBytes(1);
            }
        }
        return -1;
    }
}
