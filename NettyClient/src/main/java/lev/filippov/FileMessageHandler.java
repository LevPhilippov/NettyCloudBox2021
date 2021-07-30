package lev.filippov;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileMessageHandler extends ChannelInboundHandlerAdapter {

    Logger logger;

    public FileMessageHandler() {
        super();
        this.logger = LogManager.getLogger(this.getClass().getName());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof FileMessage) {
            ClientUtils.getFromChannelToFile((FileMessage) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage());
        ctx.channel().close();
    }
}
