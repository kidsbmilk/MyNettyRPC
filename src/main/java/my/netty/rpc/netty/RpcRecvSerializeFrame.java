package my.netty.rpc.netty;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import io.netty.channel.ChannelPipeline;
import my.netty.rpc.netty.handler.*;
import my.netty.rpc.serialize.RpcSerializeFrame;
import my.netty.rpc.serialize.RpcSerializeProtocol;

import java.util.Map;

public class RpcRecvSerializeFrame implements RpcSerializeFrame {

    private Map<String, Object> handlerMap = null;

    public RpcRecvSerializeFrame(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    private static ClassToInstanceMap <NettyRpcRecvHandler> handler = MutableClassToInstanceMap.create();
    // Guava ClassToInstanceMap
    // https://www.cnblogs.com/zemliu/p/3335982.html

    static {
        handler.putInstance(JdkNativeRecvHandler.class, new JdkNativeRecvHandler());
        handler.putInstance(KryoRecvHandler.class, new KryoRecvHandler());
        handler.putInstance(HessianRecvHandler.class, new HessianRecvHandler());
        handler.putInstance(ProtostuffRecvHandler.class, new ProtostuffRecvHandler());
    }

    @Override
    public void select(RpcSerializeProtocol protocol, ChannelPipeline pipeline) {
        switch (protocol) {
            case JDKSERIALIZE: {
                handler.getInstance(JdkNativeRecvHandler.class).handle(handlerMap, pipeline);
                break;
            }
            case KRYOSERIALIZE: {
                handler.getInstance(KryoRecvHandler.class).handle(handlerMap, pipeline);
                break;
            }
            case HESSIANSERIALIZE: {
                handler.getInstance(HessianRecvHandler.class).handle(handlerMap, pipeline);
                break;
            }
            case PROTOSTUFFSERIALIZE: {
                handler.getInstance(ProtostuffRecvHandler.class).handle(handlerMap, pipeline);
                break;
            }
            default:{
                break;
            }
        }
    }
}
