package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Date;

public class EchoServer {

    public static void main(String[] args) throws Exception {

        int coreCount = Runtime.getRuntime().availableProcessors();

        ServerBootstrap bootstrap = new ServerBootstrap();

        EventLoopGroup parentGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(coreCount);

        EchoServerHandler serverHandler = new EchoServerHandler();

        bootstrap.group(parentGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO));

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                socketChannel.pipeline().addLast(serverHandler);
            }
        });

        try {
            ChannelFuture channelFuture = bootstrap.bind(3421).sync();

            channelFuture.channel().closeFuture().sync();
        } finally {
            parentGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}

@ChannelHandler.Sharable // we can register and share with multiple clients
class EchoServerHandler extends ChannelInboundHandlerAdapter {

    void print(Object o) {
        System.out.println(new Date() + "|" + o.toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //print("channelRead msg:" + msg.toString());
        ctx.write(msg);
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        //print("channelReadComplete ctx:" + ctx.toString());
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
