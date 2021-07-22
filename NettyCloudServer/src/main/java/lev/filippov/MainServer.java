package lev.filippov;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class MainServer {
    public static void main(String[] args) {
        new MainServer().start();
    }

    private void start() {
        try {
            Class.forName("lev.filippov.PersistanceBean");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup child = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss,child)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(
                            new ObjectDecoder(Constants.MAX_MESSAGE_SIZE, ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(),
                            new AuthHandler(),
                            new ServiceMessageHandler(),
                            new FileMessageHandler());
                }
            }).childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture future = b.bind(8189).sync();
                future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            child.shutdownGracefully();
        }

    }

}
