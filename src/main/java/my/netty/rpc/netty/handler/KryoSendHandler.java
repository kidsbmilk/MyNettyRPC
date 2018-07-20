package my.netty.rpc.netty.handler;

import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.MessageSendHandler;
import my.netty.rpc.serialize.kryo.KryoCodecUtil;
import my.netty.rpc.serialize.kryo.KryoDecoder;
import my.netty.rpc.serialize.kryo.KryoEncoder;
import my.netty.rpc.serialize.kryo.KryoPoolFactory;

public class KryoSendHandler implements NettyRpcSendHandler {

    public void handle(ChannelPipeline pipeline) {
        KryoCodecUtil util = new KryoCodecUtil(KryoPoolFactory.getKryoPoolInstance());
        pipeline.addLast(new KryoEncoder(util));
        pipeline.addLast(new KryoDecoder(util));
        pipeline.addLast(new MessageSendHandler());
    }
}
