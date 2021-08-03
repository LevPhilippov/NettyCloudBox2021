package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ServiceMessageHandler extends ChannelInboundHandlerAdapter {

    private Logger logger;

    public ServiceMessageHandler() {
        super();
        this.logger = LogManager.getLogger(this.getClass().getName());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ServiceMessage) {
            ServiceMessage sm = (ServiceMessage) msg;
            switch (sm.getMessageType()) {
                case GET_FILE:
                    ServerUtils.writeToChannelManager(ctx,sm);
                    break;
                case FILES_LIST:
                    ServerUtils.sendFilesListManager(ctx, sm);
                    break;
                case CREATE_FOLDER:
                    ServerUtils.createFolder(ctx, sm);
                    break;
                case REMOVE:
                    ServerUtils.remove(ctx, sm);
                    break;
                default:
                    System.out.println("Unknown command: " + sm.getMessageType());
                    break;
            }
        } else
            ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        ctx.close();
    }
}
