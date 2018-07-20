package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageRecvHandler;
import my.netty.rpc.serialize.hessian.HessianCodecUtil;
import my.netty.rpc.serialize.hessian.HessianDecoder;
import my.netty.rpc.serialize.hessian.HessianEncoder;

import java.util.Map;

public class HessianRecvHandler implements NettyRpcRecvHandler {

    public void handle(Map<String, Object> handlerMap, ChannelPipeline pipeline) {
        HessianCodecUtil util = new HessianCodecUtil();
        pipeline.addLast(new HessianEncoder(util));
        pipeline.addLast(new HessianDecoder(util));
        pipeline.addLast(new MessageRecvHandler(handlerMap));
    }
}
