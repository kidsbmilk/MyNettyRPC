package my.netty.rpc.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import my.netty.rpc.serialize.support.RpcSerializeProtocol;

import java.util.Map;

/**
 * Rpc服务端管道初始化
 */
public class MessageRecvChannelInitializer extends ChannelInitializer<SocketChannel> {

    private RpcSerializeProtocol protocol;
    private RpcRecvSerializeFrame frame = null;

    MessageRecvChannelInitializer buildRpcSerializeProtocol(RpcSerializeProtocol protocol) {
        this.protocol = protocol;
        return this;
    }

    MessageRecvChannelInitializer(Map<String, Object> handlerMap) {
        frame = new RpcRecvSerializeFrame(handlerMap);
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        frame.select(protocol, pipeline);
    }
}
