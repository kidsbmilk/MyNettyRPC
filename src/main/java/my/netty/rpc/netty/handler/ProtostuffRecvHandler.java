package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageRecvHandler;
import my.netty.rpc.serialize.protostuff.ProtostuffCodecUtil;
import my.netty.rpc.serialize.protostuff.ProtostuffDecoder;
import my.netty.rpc.serialize.protostuff.ProtostuffEncoder;

import java.util.Map;

public class ProtostuffRecvHandler implements NettyRpcRecvHandler {

    public void handle(Map<String, Object> handlerMap, ChannelPipeline pipeline) {
        ProtostuffCodecUtil util = new ProtostuffCodecUtil();
        util.setRpcDirect(true);
        pipeline.addLast(new ProtostuffEncoder(util));
        pipeline.addLast(new ProtostuffDecoder(util));
        pipeline.addLast(new MessageRecvHandler(handlerMap));
    }
}
