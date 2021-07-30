package lev.filippov;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainServer {

    private final Logger logger;
    private Channel channel;
    private final String SERVER_RELATIVE_PATH;
    private static MainServer mainServer;

    private MainServer(String SERVER_RELATIVE_PATH) {
        this.logger = LogManager.getLogger(this.getClass().getName());
        this.SERVER_RELATIVE_PATH = SERVER_RELATIVE_PATH;
    }

    public static void main(String[] args) {
        mainServer = new MainServer(args[0]);
        try {
            Class.forName("lev.filippov.ServerUtils");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        mainServer.start();
    }

    public static MainServer getInstance(){
        if (mainServer == null)
            throw new RuntimeException("Server isn't runned!");
        return mainServer;
    }

    protected String getSERVER_RELATIVE_PATH() {
        return SERVER_RELATIVE_PATH;
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

                ChannelFuture f = b.bind(8189).sync();
                this.channel = f.channel();
                child.execute(this::startCommandLine);
                channel.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            child.shutdownGracefully();
        }

    }

    private void startCommandLine() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Welcome to server command line!");
            while(!Thread.currentThread().isInterrupted()){
                String line = reader.readLine();
                if (line.matches("^shutdown")) {
                    Thread.currentThread().interrupt();
                    channel.close();
                    continue;
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        System.out.println("Server is shutting down.\nCommand line is off.");
    }

}
