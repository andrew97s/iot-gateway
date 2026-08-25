package com.zhian.gateway.third.others.mk.protocol;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import com.zhian.gateway.third.common.BasePlatformHandler;
import com.zhian.gateway.third.others.mk.constants.MkConsts;
import com.zhian.gateway.third.others.mk.vo.MkCanMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 铭控TCP服务
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
     * 启动服务
     *
     * @param handler the handler
     * @throws Exception the exception
     */
    public static void start(BasePlatformHandler<MkCanMsg> handler) throws Exception {
        try {
            if (bossGroup == null || bossGroup.isShutdown()) {
                bossGroup = new NioEventLoopGroup();
            }
            if (workerGroup == null || workerGroup.isShutdown()) {
                workerGroup = new NioEventLoopGroup();
            }
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // 指定 NIO 模式
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 配置通道处理器
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // TODO 此处可能存在沾包消息 - 需要处理沾包逻辑
                            pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    // 读取消息并打印
                                    MkCanMsg mkMsg = extractMsg(ctx , msg);
                                    // 回复客户端
                                    ByteBuf response = Unpooled.copiedBuffer("ok", CharsetUtil.UTF_8);
                                    ctx.writeAndFlush(response);
                                    // 异步处理消息
                                    CompletableFuture.runAsync(
                                            () -> handler.processMsg(mkMsg)
                                    ).exceptionally(e->{
                                        log.error("处理铭控消息失败,msg:{}" ,e .getMessage());
                                        return null;
                                    });
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
        if (channel != null && channel.isOpen()) {
            channel.close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info(" MkServer 已成功停止 !");
        }
        channel = null;
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
     * 提取消息数据
     *
     * @param buf the buf
     * @return the string
     */
    public static MkCanMsg extractMsg(ChannelHandlerContext ctx, ByteBuf buf) {
        // 读取消息
        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        // 消息固定长度为22个字节
        if (length != 22) {
            log.error("铭控消息解析失败,消息长度({})非法!", length);
            return null;
        }

        MkCanMsg msg = new MkCanMsg();

        // step1: 提取设备编码(前15位)
        msg.setDeviceCode(new String(ArrayUtil.sub(bytes,0,15)));
        // step2: 提取通道号（16位）
        msg.setChannel((int)bytes[15] + "");
        // step3: 提取消息类型（17位）
        msg.setType((int)bytes[16] == 3 ? MkConsts.MSG_TYPE_MONITOR : MkConsts.MSG_TYPE_ALARM);
        // step4-1: 提取监测消息值（19+20位组成）
        if (MkConsts.MSG_TYPE_MONITOR.equals(msg.getType())) {
            // 将两个字节合并为一个 32 位的整数（int）
            int value = ((bytes[18] << 8) | (bytes[19] & 0xFF)) / 1000;
            msg.setValue(value + "");
        }
        // step4-2: 提取告警消息值（18位组成）
        else {
            msg.setValue((int)bytes[17] + "");
        }
        // TODO step5: 校验CRC

        // step6: 设置消息源IP
        msg.setSourceIp(
                ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress()
        );
        log.info("解析得到 MkCanMsg: {}" , JSON.toJSONString(msg));

        return msg;
    }
}
