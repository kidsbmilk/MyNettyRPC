package my.netty.rpc.netty;

import com.google.common.reflect.Reflection;
import my.netty.rpc.serialize.RpcSerializeProtocol;

public class MessageSendExecutor {

    private static class MessageSendExecutorHolder {
        private static final MessageSendExecutor instance = new MessageSendExecutor();
    }

    public static MessageSendExecutor getInstance() {
        return MessageSendExecutorHolder.instance;
    }

    private RpcServerLoader loader = RpcServerLoader.getInstance();

    public MessageSendExecutor() {
    }

    public MessageSendExecutor(String serverAddress, RpcSerializeProtocol serializeProtocol) {
        loader.load(serverAddress, serializeProtocol);
    }

    public void setRpcServerLoader(String serverAddress, RpcSerializeProtocol serializeProtocol) {
        loader.load(serverAddress, serializeProtocol);
    }

    public void stop() {
        loader.unLoad();
    }

    public static <T> T execute(Class<T> rpcInterface) {
        return (T) Reflection.newProxy(rpcInterface, new MessageSendProxy<T>()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
    }
}
