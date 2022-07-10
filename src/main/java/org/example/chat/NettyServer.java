package org.example.chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NettyServer {

    public static void print(Object o) {
        System.out.println(new Date() + "|" + o);
    }

    public static void main(String[] args) {
        ServerBootstrap bootstrap;
        EventLoopGroup bossGroup, workerGroup;

        int port = 3421;

        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());

        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

        bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline channelPipeline = socketChannel.pipeline();

                channelPipeline.addLast("frame delimiter", new DelimiterBasedFrameDecoder(256, Delimiters.lineDelimiter()));

                channelPipeline.addLast("String Decoder", new StringDecoder());

                channelPipeline.addLast("Server Handler", new ServerHandler());

                channelPipeline.addLast("String Encoder", new StringEncoder());

            }
        });

        try {

            ChannelFuture channelFuture = bootstrap.bind(port).sync();

            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) {
                    print("operationComplete");
                }
            });

            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class ServerHandler extends SimpleChannelInboundHandler<String> {

    private static final List<Channel> channels = new ArrayList<>();

    public static void print(Object o) {
        System.out.println(new Date() + "|" + o);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx)  {
        print(ctx.channel() + " is added");
        channels.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        print(ctx.channel() + " is removed");
        channels.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s)  {
        print("Message received from " + ctx.channel().remoteAddress() + " and is :" + s + "\n");

        for(Channel ch : channels) {
           if(ch != ctx.channel()) {
               ch.writeAndFlush(ctx.channel().remoteAddress() + ":" + s + "\n");
           }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        print("Closing connection for " + ctx + " due to " + cause);
        ctx.close();
    }
}
