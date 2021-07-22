package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;
import static lev.filippov.ServerUtils.*;


public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            Map<String, Object> params = (sm.getParametersMap());
            switch (sm.getMessageType()) {
                case GET_FILE:
                    ServerUtils.writeToChannel(ctx,sm);
                    break;
                case GET_STRUCTURE:
                    ServerUtils.sendFilesList(ctx, sm);
                    break;
                default:
                    System.out.println("Unknown command " + sm.getMessageType());
                    break;
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
