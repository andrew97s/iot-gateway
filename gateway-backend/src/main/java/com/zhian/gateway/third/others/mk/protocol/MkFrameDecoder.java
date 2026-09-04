package com.zhian.gateway.third.others.mk.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 铭控粘包拆包：V3.3 按 A55A+长度+55AA 切帧，青鸟定制协议按 @@ 切帧，原气瓶协议按 22 字节定长切帧
 *
 * @author tongwenjin
 * @since 2026/9/3
 */
@Slf4j
public class MkFrameDecoder extends ByteToMessageDecoder {

    static final int CAN_FRAME_LEN = 22;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 2) {
            return;
        }
        int b0 = in.getByte(in.readerIndex()) & 0xFF;
        int b1 = in.getByte(in.readerIndex() + 1) & 0xFF;
        if (b0 == 0xA5 && b1 == 0x5A) {
            int frameLen = MkV3Protocol.estimateFrameLength(in);
            if (frameLen == -1) {
                if (in.readableBytes() > MkV3Protocol.MAX_FRAME_LEN) {
                    log.error("铭控V3协议帧超长, 丢弃 {} 字节", in.readableBytes());
                    in.skipBytes(in.readableBytes());
                }
                return;
            }
            if (frameLen == -2) {
                log.error("铭控V3协议帧非法, 跳过起始标记");
                in.skipBytes(2);
                return;
            }
            out.add(in.readRetainedSlice(frameLen));
            return;
        }
        if (b0 == MkJBProtocol.START[0] && b1 == MkJBProtocol.START[1]) {
            out.add(in.readRetainedSlice(in.readableBytes()));
            return;
        }
        if (in.readableBytes() == CAN_FRAME_LEN) {
            out.add(in.readRetainedSlice(CAN_FRAME_LEN));
        }
        else {
            log.error("铭控协议无效, 丢弃 {} 字节", in.readableBytes());
            in.skipBytes(in.readableBytes());
        }
    }
}
