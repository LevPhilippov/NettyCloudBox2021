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
    public static String currentRemoteFolderPath = "root";

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
            showHelp();

            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    private void showHelp() {
        System.out.println("Command line is runned! " +
                "For authorization in system please type: auth login [login] pass [password] where words " +
                "with brackets are your login and password in the system!\nTo ask files list type: gs [folder/]\nTo send" +
                "file type: send localfolder(or file) to folder\nTo get file or folder type: get folder(or file) to folder\n" +
                "To delete file or folder on the serverside type: remove remotepathfolder/\n" +
                "To create folder on the severside type: create foldername in remotefolderpath/");
        System.out.println("");
    }

    private void startCommandLine() {
        try(InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr))
        {
            while (true) {
                if(Thread.currentThread().isInterrupted()){
                    logger.info("Closing application. Commandline is off.");
                    break;
                }
                //bloking oparetion
                String line = reader.readLine();
                //shutdown operation
                if (line.startsWith("shutdown")) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                if (line.startsWith("help")){
                    showHelp();
                    continue;
                }
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
                } /*when you already authorized in a system and authKey is present*/
                else {
                    String[] tokens = line.split("\\s");

                    if (tokens[0].equals("gs")) {
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.GET_STRUCTURE);
                        if (tokens.length == 2) {
                            sm.getParametersMap().put(REMOTE_PATH,tokens[1]);
                        } else {
                            sm.getParametersMap().put(REMOTE_PATH, "root");
                        }
                        channel.writeAndFlush(sm);
                    } else if(tokens[0].equals("get") && tokens[2].equals("to")) {
                        //query must be like {get folder(or file) to folder\}
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.GET_FILE);
                            if (tokens.length > 3) {
                                sm.getParametersMap().put(LOCAL_PATH, tokens[3]);
                            } else {
                                sm.getParametersMap().put(LOCAL_PATH, "");
                            }
                        sm.getParametersMap().put(REMOTE_PATH, tokens[1]);
                        channel.writeAndFlush(sm);
                    }  else if (tokens[0].equals("send") && tokens[2].equals("to"))   {
                        //query must be like {send localfolder(or file) to folder\}
                        if (tokens.length > 3) {
                            ClientUtils.writeToChannelManager(channel,tokens[1], tokens[3], authKey);
                        } else {
                            ClientUtils.writeToChannelManager(channel,tokens[1], new String(""), authKey);
                        }
                    } else if (tokens[0].equals("create") && tokens[2].equals("in") || tokens[2].equals("to") ){
                        //query must be like {create folder/ in remotepathfolder/ }
                        ClientUtils.createRemoteDirectory(channel, tokens[1], tokens[3], authKey);
                    } else if (tokens[0].equals("remove")){
                        //query must be like {remove remotepathfolder/ }
                        ClientUtils.removeRemote(channel, tokens[1], authKey);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        channel.close();
    }


    public AuthKey getAuthKey() {
        return authKey;
    }

    public void setAuthKey(AuthKey authKey) {
        this.authKey = authKey;
    }
}
