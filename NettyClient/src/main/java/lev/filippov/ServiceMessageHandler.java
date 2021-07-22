package lev.filippov;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

import static lev.filippov.Constants.FILES_LIST;
import static lev.filippov.Constants.REMOTE_PATH;

public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    NettyClient client;
    Logger logger;

    public ServiceMessageHandler(NettyClient client) {
        this.client = client;
        this.logger = LogManager.getLogger(this.getClass().getName());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        autoLogIn(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthKey){
            client.setAuthKey((AuthKey) msg);
            logger.info("Получен валидный экземпляр AuthKey.");
        }
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            switch (sm.getMessageType()){
                case GET_STRUCTURE:
                    NettyClient.currentRemoteFolderPath = (String) sm.getParametersMap().get(REMOTE_PATH);
                    List<String> filesList = (List<String>) sm.getParametersMap().get(FILES_LIST);
                    System.out.print("Current folder is: " + NettyClient.currentRemoteFolderPath + "\n");
                    for (String s : filesList) {
                        System.out.println(s);
                    }
                    break;
                default:
                    System.out.println("Unknown message format!");
                    break;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private void autoLogIn(ChannelHandlerContext ctx) throws IOException {
        System.out.println("Autolog");
        AuthKey authKey = new AuthKey();
        authKey.setLogin("qwerty");
        authKey.setPassword(DigestUtils.md5Hex("12345"));
        ctx.writeAndFlush(authKey);
    }

}
