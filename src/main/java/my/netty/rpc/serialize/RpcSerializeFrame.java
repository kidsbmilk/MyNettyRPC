package my.netty.rpc.serialize;

import io.netty.channel.ChannelPipeline;

public interface RpcSerializeFrame {

    public void select(RpcSerializeProtocol protocol, ChannelPipeline pipeline);
}
