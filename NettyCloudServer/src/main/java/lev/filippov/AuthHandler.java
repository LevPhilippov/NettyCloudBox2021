package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.sql.Timestamp;
import java.util.UUID;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private AuthKey authKey;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof AuthKey) {
            AuthKey authKey = (AuthKey) msg;
            if(PersistanceBean.isUserExist(authKey.getLogin(), authKey.getPassword())){
                authKey.setUuid(UUID.randomUUID());
                authKey.setTimestamp(new Timestamp(System.currentTimeMillis()));
                this.authKey = authKey;
                ctx.writeAndFlush(authKey);
            }
        } else {
            Message message = (Message) msg;
            if (message.getAuthKey().equals(authKey))
                ctx.fireChannelRead(msg);
        }
    }
}
