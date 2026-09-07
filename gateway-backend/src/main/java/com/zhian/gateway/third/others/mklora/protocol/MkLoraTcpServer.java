package com.zhian.gateway.third.others.mklora.protocol;

import com.zhian.gateway.third.others.mklora.vo.MkLoraReportMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 铭控 LoRa 上报 TCP 服务（网关侧监听，设备/网关主动推送）。
 */
@Slf4j
public class MkLoraTcpServer {

    private final int port;
    private final Consumer<MkLoraReportMsg> messageConsumer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public MkLoraTcpServer(int port, Consumer<MkLoraReportMsg> messageConsumer) {
        this.port = port;
        this.messageConsumer = messageConsumer;
    }

    public synchronized void start() throws InterruptedException {
        stop();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MkLoraFrameDecoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<MkLoraReportMsg>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, MkLoraReportMsg msg) {
                                if (msg.getSourceIp() == null && ctx.channel().remoteAddress() instanceof InetSocketAddress) {
                                    msg.setSourceIp(((InetSocketAddress) ctx.channel().remoteAddress())
                                            .getAddress().getHostAddress());
                                }
                                log.info("解析帧:{}" , msg);
                                CompletableFuture.runAsync(() -> {
                                    try {
                                        messageConsumer.accept(msg);
                                    } catch (Exception e) {
                                        log.error("处理铭控LoRa上报失败: {}", e.getMessage(), e);
                                    }
                                });
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.warn("铭控LoRa连接异常: {}", cause.getMessage());
                                ctx.close();
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        channel = bootstrap.bind(port).sync().channel();
        log.info("MkLoraTcpServer(port: {}) 启动成功", port);
    }

    public synchronized void stop() {
        if (channel != null) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Exception e) {
                log.warn("关闭铭控LoRa服务通道失败: {}", e.getMessage());
            }
            channel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public int getPort() {
        return port;
    }
}
