package com.zhian.gateway.third.others.mk;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.others.mk.constants.MkConsts;
import com.zhian.gateway.third.others.mk.protocol.MkFrameDecoder;
import com.zhian.gateway.third.others.mk.protocol.MkJBProtocol;
import com.zhian.gateway.third.others.mk.protocol.MkV3Protocol;
import com.zhian.gateway.third.others.mk.vo.MkV3Msg;
import com.zhian.gateway.third.others.mk.vo.MkJBMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 铭控TCP服务，包含以下三类协议实现<br/>
 * 1.铭控无线设备协议见<a href="https://docs.qq.com/pdf/DVnJSWXVNVWRGcGRw">无线协议文档</a><br>
 * 2.铭控青鸟定制协议文档见<a href="https://docs.qq.com/sheet/DVkpUbGpZYWVmVkVw?tab=000001">青鸟定制协议文档</a><br>
 * 3.铭控气瓶协议
 *
 *
 * @author tongwenjin
 * @since 2024 -12-4
 */
@Slf4j
public class MkServer {

    /**
     * The constant PORT.
     */
    public static final int PORT = 9210;

    /**
     * The constant channel.
     */
    private static Channel channel;
    /**
     * The constant bossGroup.
     */
    private static EventLoopGroup bossGroup = new NioEventLoopGroup();
    /**
     * The constant workerGroup.
     */
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    /**
     * 青鸟定制协议设备连接，key=设备编号
     */
    private static final ConcurrentHashMap<String, Channel> DEVICE_CHANNELS = new ConcurrentHashMap<>();

    /**
     * 启动服务
     *
     * @param handler the handler
     * @throws Exception the exception
     */
    public static void start(BasePlatformHandler<?> handler) throws Exception {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 指定 NIO 模式
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 配置通道处理器
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new MkFrameDecoder());
                            pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    Object mkMsg = extractMsg(ctx, msg);
                                    reply(ctx, mkMsg);
                                    bindChannel(ctx, mkMsg);
                                    CompletableFuture.runAsync(
                                            () -> handler.processMsg(mkMsg)
                                    ).exceptionally(e -> {
                                        log.error("处理铭控消息失败,msg:{}", e.getMessage());
                                        return null;
                                    });
                                }

                                @Override
                                public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                                    if (evt instanceof IdleStateEvent
                                            && ((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                                        log.info("铭控设备5秒无后续报文, 主动断开 {}", ctx.channel().remoteAddress());
                                        ctx.close();
                                        return;
                                    }
                                    ctx.fireUserEventTriggered(evt);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128) // 配置连接队列大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // 启用保持活动连接

            // 绑定端口并启动服务器
            channel = bootstrap.bind(PORT).sync().channel();
            log.info("MkServer(port: {}) 启动成功!", PORT);
        } catch (Exception e) {
            log.error("MkServer 发生异常,msg:{}", e.getMessage());
            MkServer.stop();
        }
    }

    /**
     * 停止服务
     */
    public static void stop() {
        DEVICE_CHANNELS.clear();
        if (channel != null && channel.isOpen()) {
            channel.close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info(" MkServer 已成功停止 !");
        }
    }

    /**
     * 判断当前服务是否正常
     *
     * @return the boolean
     */
    public static boolean isActive() {
        return channel != null && channel.isActive();
    }

    /**
     * 按设备编号获取在线连接
     *
     * @param deviceCode the device code
     * @return the device channel
     */
    public static Channel getDeviceChannel(String deviceCode) {
        return DEVICE_CHANNELS.get(deviceCode);
    }

    /**
     * 向设备下发青鸟定制指令
     *
     * @param deviceCode the device code
     * @param payload    the payload
     * @return the boolean
     */
    public static boolean send(String deviceCode, byte[] payload) {
        Channel deviceChannel = DEVICE_CHANNELS.get(deviceCode);
        if (deviceChannel == null || !deviceChannel.isActive()) {
            return false;
        }
        deviceChannel.writeAndFlush(Unpooled.wrappedBuffer(payload));
        return true;
    }

    /**
     * 提取消息数据
     *
     * @param ctx the ctx
     * @param buf the buf
     * @return the parsed message
     */
    public static Object extractMsg(ChannelHandlerContext ctx, ByteBuf buf) {
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        String sourceIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

        if (MkV3Protocol.isV3Frame(bytes)) {
            log.debug("尝试解析MK_V3消息:{}", HexUtil.encodeHexStr(bytes));
            MkV3Msg v3Msg = MkV3Protocol.parse(bytes);
            if (v3Msg == null) {
                log.error("铭控V3协议解析失败, len={}", length);
                return null;
            }
            v3Msg.setSourceIp(sourceIp);
            log.info("解析得到 MkV3Msg: {}", JSON.toJSONString(v3Msg));
            return v3Msg;
        }

        if (MkJBProtocol.isJbFrame(bytes)) {
            log.info( "尝试解析MK_JB消息:{}",HexUtil.encodeHexStr(bytes));
            MkJBMsg jbMsg = MkJBProtocol.parse(bytes);
            if (jbMsg == null) {
                log.error("铭控青鸟协议解析失败, len={}", length);
                return null;
            }
            jbMsg.setSourceIp(sourceIp);
            log.info("解析得到 MkJBMsg: {}", JSON.toJSONString(jbMsg));
            return jbMsg;
        }

        return null;
    }

    private static void reply(ChannelHandlerContext ctx, Object mkMsg) {
        if (mkMsg instanceof MkV3Msg) {
            MkV3Msg v3Msg = (MkV3Msg) mkMsg;
            if (v3Msg.getCommand() != MkV3Protocol.CMD_UPLOAD) {
                return;
            }
            ctx.writeAndFlush(Unpooled.wrappedBuffer(MkV3Protocol.buildAck(v3Msg.getDeviceCode())));
            return;
        }
        if (mkMsg instanceof MkJBMsg) {
            MkJBMsg jbMsg = (MkJBMsg) mkMsg;
            if (jbMsg.getDataType() == MkConsts.JB_TYPE_ACK) {
                return;
            }
            ctx.writeAndFlush(Unpooled.wrappedBuffer(MkJBProtocol.buildAck(jbMsg)));
        }
    }

    private static void bindChannel(ChannelHandlerContext ctx, Object mkMsg) {
        String code = null;
        if (mkMsg instanceof MkV3Msg) {
            code = ((MkV3Msg) mkMsg).getDeviceCode();
        } else if (mkMsg instanceof MkJBMsg) {
            code = ((MkJBMsg) mkMsg).getCode();
        }
        if (code == null || code.isEmpty()) {
            return;
        }
        final String deviceCode = code;
        Channel current = ctx.channel();
        Channel old = DEVICE_CHANNELS.put(deviceCode, current);
        if (old != current) {
            current.closeFuture().addListener(future -> DEVICE_CHANNELS.remove(deviceCode, current));
        }
    }
}
