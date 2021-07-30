package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Timestamp;
import java.util.UUID;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private AuthKey authKey;
    private Logger logger;

    public AuthHandler() {
        this.logger = LogManager.getLogger(this.getClass().getName());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("New client is connected.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthKey) {
            logger.info("Authorization ask has earned.");
            AuthKey authKey = (AuthKey) msg;
            if(PersistanceBean.isUserExist(authKey.getLogin(), authKey.getPassword())){
                authKey.setUuid(UUID.randomUUID());
                authKey.setTimestamp(new Timestamp(System.currentTimeMillis()));
                this.authKey = authKey;
                ctx.writeAndFlush(authKey);
                logger.info("Authorization complete.");
                ServerUtils.createUserFolder(authKey.getLogin());
            }
        } else {
            Message message = (Message) msg;
            if (message.getAuthKey().equals(authKey))
                ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        ctx.close();
    }
}
