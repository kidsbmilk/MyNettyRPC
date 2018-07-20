package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageRecvHandler;
import my.netty.rpc.serialize.kryo.KryoCodecUtil;
import my.netty.rpc.serialize.kryo.KryoDecoder;
import my.netty.rpc.serialize.kryo.KryoEncoder;
import my.netty.rpc.serialize.kryo.KryoPoolFactory;

import java.util.Map;

public class KryoRecvHandler implements NettyRpcRecvHandler {

    public void handle(Map<String, Object> handlerMap, ChannelPipeline pipeline) {
        KryoCodecUtil util = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());
        pipeline.addLast(new KryoEncoder(util));
        pipeline.addLast(new KryoDecoder(util));
        pipeline.addLast(new MessageRecvHandler(handlerMap));
    }
}
