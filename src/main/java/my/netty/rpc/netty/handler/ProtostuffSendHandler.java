package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageSendHandler;
import my.netty.rpc.serialize.protostuff.ProtostuffCodecUtil;
import my.netty.rpc.serialize.protostuff.ProtostuffDecoder;
import my.netty.rpc.serialize.protostuff.ProtostuffEncoder;

public class ProtostuffSendHandler implements NettyRpcSendHandler {

    @Override
    public void handle(ChannelPipeline pipeline) {
        ProtostuffCodecUtil util = new ProtostuffCodecUtil();
        util.setRpcDirect(false);
        pipeline.addLast(new ProtostuffEncoder(util));
        pipeline.addLast(new ProtostuffDecoder(util));
        pipeline.addLast(new MessageSendHandler());
    }
}
