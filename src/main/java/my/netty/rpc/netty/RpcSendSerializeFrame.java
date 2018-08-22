package my.netty.rpc.netty;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.handler.*;
import my.netty.rpc.serialize.RpcSerializeFrame;
import my.netty.rpc.serialize.RpcSerializeProtocol;

public class RpcSendSerializeFrame implements RpcSerializeFrame {

    private static ClassToInstanceMap<NettyRpcSendHandler> handler = MutableClassToInstanceMap.create();

    static {
        handler.putInstance(JdkNativeSendHandler.class, new JdkNativeSendHandler());
        handler.putInstance(KryoSendHandler.class, new KryoSendHandler());
        handler.putInstance(HessianSendHandler.class, new HessianSendHandler());
        handler.putInstance(ProtostuffSendHandler.class, new ProtostuffSendHandler());
    }

    @Override
    public void select(RpcSerializeProtocol protocol, ChannelPipeline pipeline) {
        switch (protocol) {
            case JDKSERIALIZE: {
                handler.getInstance(JdkNativeSendHandler.class).handle(pipeline);
                break;
            }
            case KRYOSERIALIZE: {
                handler.getInstance(KryoSendHandler.class).handle(pipeline);
                break;
            }
            case HESSIANSERIALIZE: {
                handler.getInstance(HessianSendHandler.class).handle(pipeline);
                break;
            }
            case PROTOSTUFFSERIALIZE: {
                handler.getInstance(ProtostuffSendHandler.class).handle(pipeline);
                break;
            }
            default:{
                break;
            }
        }
    }
}
