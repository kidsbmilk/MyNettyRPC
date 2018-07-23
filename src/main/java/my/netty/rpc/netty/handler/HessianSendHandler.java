package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageSendHandler;
import my.netty.rpc.serialize.hessian.HessianCodecUtil;
import my.netty.rpc.serialize.hessian.HessianDecoder;
import my.netty.rpc.serialize.hessian.HessianEncoder;

public class HessianSendHandler implements NettyRpcSendHandler {

    public void handle(ChannelPipeline pipeline) {
        HessianCodecUtil util = new HessianCodecUtil();
        pipeline.addLast(new HessianEncoder(util));
        pipeline.addLast(new HessianDecoder(util));
        pipeline.addLast(new MessageSendHandler());
    }
}
