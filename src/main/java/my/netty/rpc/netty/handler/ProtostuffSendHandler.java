package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageSendHandler;

public class ProtostuffSendHandler implements NettyRpcSendHandler {

    public void handle(ChannelPipeline pipeline) {
        ProtostuffCodecUtil util = new ProtostuffCodecUtil();
        util.setRpcDirect(false);
        pipeline.addLast(new ProtostuffEncoder(util));
        pipeline.addLast(new ProtostuffDecoder(util));
        pipeline.addLast(new MessageSendHandler());
    }
}
