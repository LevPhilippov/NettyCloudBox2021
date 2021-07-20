package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;
import static lev.filippov.ServerUtils.*;


public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServiceMessage) {
            Map<String, Object> params = ((ServiceMessage)msg).getParametersMap();
            switch (((ServiceMessage) msg).getMessageType()){
                case GET_FILE:
                    ServerUtils.writeToChannel(ctx,(ServiceMessage) msg);
                    break;
                default: break;
            }
        } else
            ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
