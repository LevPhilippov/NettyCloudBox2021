package lev.filippov;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

import static lev.filippov.Constants.LOCAL_PATH;
import static lev.filippov.Constants.REMOTE_PATH;

public class NettyClient {

    private final String url;
    private final int port;
    private EventLoopGroup workerGroup;
    private AuthKey authKey;
    private Logger logger = LogManager.getLogger(this.getClass().getName());
    private Channel channel;
    public static String currentRemoteFolderPath = "";

    public NettyClient(String url, int port) {
        this.url = url;
        this.port = port;
    }

    public static void main(String[] args) {
        new NettyClient("localhost", 8189).start();
    }

    private void start() {
        try {
            workerGroup = new NioEventLoopGroup();
            workerGroup.execute(this::startCommandLine);
            Bootstrap b = new Bootstrap();
            b.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new ObjectDecoder(Constants.MAX_MESSAGE_SIZE, ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(),
                            new ServiceMessageHandler(NettyClient.this),
                            new FileMessageHandler());
                }
            }).option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.connect(url, port).sync();
            channel = f.channel();

            logger.info("Client is ready for use!");
            System.out.println("Command line is runned! For authorization in system please type: auth login [login] pass [password] where words " +
                    "with brackest are your login and password in the system!");

            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    private void startCommandLine() {
        //TODO: сделать рефактор на читаемый код
        try(InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr))
        {
            while (true) {
                if(Thread.currentThread().isInterrupted()){
                    break;
                }
                //bloking oparetion
                String line = reader.readLine();
                //when not authorized in a system
                if(authKey == null) {
                    //encounter only auth query
                    if(line.startsWith("auth")) {
                        logger.info("Получена команда на авторизацию.");
                        AuthKey tempAuthKey = new AuthKey();
                        String[] strings = line.split("\\s");
                        Iterator<String> iter = Arrays.stream(strings).iterator();
                        String prev=null;
                        String cur=null;
                        while (iter.hasNext()) {
                            cur = iter.next();
                            if (prev != null) {
                                if (prev.equals("login") && tempAuthKey.getLogin() == null){
                                    tempAuthKey.setLogin(cur);
                                }
                                if (prev.equals("pass") && tempAuthKey.getPassword() == null){
                                    tempAuthKey.setPassword(DigestUtils.md5Hex(cur));
                                }
                            }
                            prev=cur;
                        }
                        if (tempAuthKey.getLogin() != null && tempAuthKey.getPassword()!= null) {
                            channel.writeAndFlush(tempAuthKey);
                            logger.info("Отправлены данные авторизации.");
                        }
                    }
                } /*when you already authorized in a system*/
                else {
                    String[] tokens = line.split("\\s");

                    if (tokens[0].equals("dl")) {
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.GET_STRUCTURE);
                        if (tokens.length == 2) {
                            sm.getParametersMap().put(REMOTE_PATH,tokens[1]);
                        } else {
                            sm.getParametersMap().put(REMOTE_PATH, currentRemoteFolderPath);
                        }
                        channel.writeAndFlush(sm);
                    } else if(tokens[0].equals("get")) {
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.GET_FILE);
                        sm.getParametersMap().put(LOCAL_PATH, "");
                        sm.getParametersMap().put(REMOTE_PATH, tokens[1]);
                        channel.writeAndFlush(sm);
                    }  else if (line.startsWith("send"))   {
//                        //todo: дописать логику с токенами и передачу нескольких файлов подряд
//                        ClientUtils.writeToChannel(encoder, Paths.get(line), Paths.get(line).getFileName().toString());
                    }
                }
                //encounter shutdown
                if (line.startsWith("shutdown")) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        workerGroup.shutdownGracefully();
    }


    public AuthKey getAuthKey() {
        return authKey;
    }

    public void setAuthKey(AuthKey authKey) {
        this.authKey = authKey;
    }
}
