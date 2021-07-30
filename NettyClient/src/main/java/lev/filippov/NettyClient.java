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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import static lev.filippov.Constants.LOCAL_PATH;
import static lev.filippov.Constants.REMOTE_PATH;

public class NettyClient {

    private final Logger logger;
    private final String url;
    private final int port;
    private EventLoopGroup workerGroup;
    private AuthKey authKey;
    private Channel channel;
    public static String currentRemoteFolderPath = "root";
    private static NettyClient nettyClient;
    private final String CLIENT_FOLDER;

    private NettyClient(String url, int port, String clientFolder) {
        this.logger = LogManager.getLogger(this.getClass().getName());
        this.url = url;
        this.port = port;
        this.CLIENT_FOLDER=clientFolder;
        NettyClient.nettyClient = this;
    }

    public static void main(String[] args) {
        new NettyClient(args[0],Integer.parseInt(args[1]),args[2]).start();
    }

    public static NettyClient getInstance(){
        if(Objects.isNull(nettyClient))
            throw new RuntimeException("NettyClient isn't runned!");
        return nettyClient;
    }

    public String getCLIENT_FOLDER() {
        return CLIENT_FOLDER;
    }

    private void start() {
        try {
            Class.forName("lev.filippov.ClientUtils");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        System.out.println("Command line is runned!\n" +
                "To use commands type and enter any command below, replacing value in square brackets with pathes of files and folder you need.\n" +
                "Text into round brackets is OPTIONAL!\n" +
                "For authorization in system type: auth l- [login] p- [password] where words with brackets are your login and password in the system!\n" +
                "To ask files list type: fl( [folder/])\n" +
                "To send file type: send [localfolder(or file)]( to [folder])\n" +
                "To get file or folder type: get [folder\\(or file)]( to [folder\\])\n" +
                "To create folder on the severside type: create [foldername\\]( in [remotefolderpath\\])"+
                "To delete file or folder on the serverside type: remove [remotepathfolder\\]\n" +
                "To see this notice again type [help]");
    }

    public AuthKey getAuthKey() {
        return authKey;
    }

    public void setAuthKey(AuthKey authKey) {
        this.authKey = authKey;
    }

    private void startCommandLine() {
        try(InputStreamReader isr = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr))
        {
            while (!Thread.currentThread().isInterrupted()) {

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
                String[] tokens = line.split("\\s");
                //when not authorized in a system
                if(authKey == null) {
                    //encounter only auth query
                    if(line.matches("^auth\\sl-\\s\\S+\\sp-\\s\\S+")) {
                        logger.info("Получена команда на авторизацию.");
                        AuthKey tempAuthKey = new AuthKey();
                        Iterator<String> iter = Arrays.stream(tokens).iterator();
//                        String prev=null;
                        String cur=null;
                        while (iter.hasNext()) {
                            cur = iter.next();
                            if(cur.matches("l-"))
                                tempAuthKey.setLogin(iter.next());
                            if(cur.matches("p-"))
                                tempAuthKey.setPassword(DigestUtils.md5Hex(iter.next()));
//                            if (prev != null) {
//                                if (prev.equals("login") && tempAuthKey.getLogin() == null){
//                                    tempAuthKey.setLogin(cur);
//                                }
//                                if (prev.equals("pass") && tempAuthKey.getPassword() == null){
//                                    tempAuthKey.setPassword(DigestUtils.md5Hex(cur));
//                                }
//                            }
//                            prev=cur;
                        }
                        if (!Objects.isNull(tempAuthKey.getLogin()) && !Objects.isNull(tempAuthKey.getPassword())) {
                            channel.writeAndFlush(tempAuthKey);
                            logger.info("Отправлены данные авторизации.");
                        }
                    }
                } /*when you already authorized in a system and authKey is present*/
                else {
                    //get files list
                    if (line.matches("^fl(\\s\\S+)?")) {
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.FILES_LIST);
                        if (tokens.length == 2) {
                            sm.getParametersMap().put(REMOTE_PATH,tokens[1].endsWith("\\") ? "" : tokens[1]+"\\");
                        } else {
                            sm.getParametersMap().put(REMOTE_PATH, null);
                        }
                        channel.writeAndFlush(sm);
                    }
                    // get files or folders
                    else if(line.matches("^get\\s\\S+(\\sto\\s\\S+)?")) {
                        //query must be like {get folder\\(or file) to folder\}
                        ServiceMessage sm = new ServiceMessage(authKey);
                        sm.setMessageType(MessageType.GET_FILE);
                            if (tokens.length == 4) {
                                sm.getParametersMap().put(LOCAL_PATH, tokens[3]);
                            } else if (tokens.length==2) {
                                sm.getParametersMap().put(LOCAL_PATH, new String(""));
                            } else {
                                System.out.println("Unknown command.\nPlease, check your query must be like {get [folder\\or file]( to folder\\)}");
                            }
                        sm.getParametersMap().put(REMOTE_PATH, tokens[1]);
                        channel.writeAndFlush(sm);
                    }
                    //send files or folders to server
                    else if (line.matches("^copy\\s\\S+(\\sto\\s\\S+)?")) {
                        //query must be like {copy localfolder\\(or file) to folder\}
                        if (tokens.length == 4) {
                            ClientUtils.writeToChannelManager(channel,tokens[1], tokens[3], authKey);
                        } else if (tokens.length ==2 ) {
                            ClientUtils.writeToChannelManager(channel,tokens[1], new String(""), authKey);
                        } else {
                            System.out.println("Unknown command.\nPlease, check your query must be like {copy [localfolder\\or file]( to folder\\)}");
                        }
                    }
                    //create directory on the server side
                    else if (line.matches("^create\\s\\S+(\\sin\\s\\S+)?")){
                        //query must be like {create folder/ in(or to) remotepathfolder/ }
                        if (tokens.length == 4) {
                            ClientUtils.createRemoteDirectory(channel, tokens[1], tokens[3], authKey);
                        } else if (tokens.length == 2){
                            ClientUtils.createRemoteDirectory(channel, tokens[1], "", authKey);
                        } else {
                            System.out.println("Unknown command.\nPlease, check your query must be like {create [folder/]( in(or to) remotepathfolder/)}");
                        }
                    }
                    //remove directory on the server side
                    else if (line.matches("^remove\\s\\S+")){
                        //query must be like {remove remotepathfolder/ }
                        logger.info("Remove query for {}", tokens[1]);
                        ClientUtils.removeRemote(channel, tokens[1], authKey);
                    } else {
                        System.out.println("Unknown command.\nPlease, type help for commands list.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Closing application. Commandline is off.");
        channel.close();
    }


}
