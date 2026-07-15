package com.zhian.gateway.third.others.mk.protocol;

import com.zhian.gateway.third.others.mk.MkHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * client
 *
 * @author tongwenjin
 * @since 2024-12-4
 */
@Slf4j
public class MkClient {

    public static void start() throws Exception {
        // 创建线程组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 客户端启动类
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group) // 配置线程组
                    .channel(NioSocketChannel.class) // 指定为 NIO 模式
                    .handler(new ChannelInitializer<SocketChannel>() { // 配置处理器
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    CompletableFuture.runAsync(()->{
                                        // 连接建立后发送消息
                                        for (int i = 0; i < 10; i++) {
                                            try {
                                                Thread.sleep(1000L);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                            String message = "Hello, Server " + i;
                                            ByteBuf buffer = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
                                            ctx.writeAndFlush(buffer);
                                        }
                                    });
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                                    // 接收服务器的响应
                                    String response = msg.toString(CharsetUtil.UTF_8);
                                    log.info("Received response from server: {}" , response);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    });

            // 连接到服务器
            ChannelFuture future = bootstrap.connect("localhost", MkServer.PORT).sync();
//            ChannelFuture future = bootstrap.connect("8.154.30.200", 8899).sync();
           log.info("Connected to server " + "localhost" + ":" + MkServer.PORT);

            // 等待客户端通道关闭
            future.channel().closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Thread(()->{
            try {
                MkServer.start(new MkHandler());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        new Thread(()->{
            try {
                MkClient.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
