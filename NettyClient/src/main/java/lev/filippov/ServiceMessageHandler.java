package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static lev.filippov.Constants.*;

public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    NettyClient client;
    Logger logger;

    public ServiceMessageHandler(NettyClient client) {
        this.client = client;
        this.logger = LogManager.getLogger(this.getClass().getName());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        autoLogIn(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthKey){
            client.setAuthKey((AuthKey) msg);
            System.out.println("Регистрация пройдена.");
        }
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            switch (sm.getMessageType()){
                case FILES_LIST:
                    ClientUtils.printFilesList(sm);
                    break;
                case MESSAGE:
                    System.out.println(sm.getParametersMap().get(MESSAGE));
                    break;
                case CREATE_FOLDER:
                    ClientUtils.createDirectory((ServiceMessage) msg);
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
        logger.error(cause.getMessage());
        ctx.channel().close();
    }

    private void autoLogIn(ChannelHandlerContext ctx) throws IOException {
        System.out.println("Autolog");
        AuthKey authKey = new AuthKey();
        authKey.setLogin("qwerty");
        authKey.setPassword(DigestUtils.md5Hex("12345"));
        ctx.writeAndFlush(authKey);
    }

}
