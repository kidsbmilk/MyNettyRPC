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

//    public static <T> T execute(Class<T> rpcInterface) {
//        return (T) Reflection.newProxy(rpcInterface, new MessageSendProxy<T>()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
//    }

    // 上面一种写法是JDK的写法，用了强制类型转换，其实下面的写法也可以，Reflection.newProxy不用强制类型转换（进而MessageSendProxy类定义中也可以去掉泛型），见PojoCallTest里的分析。
    public static Object execute(Class rpcInterface) {
        return Reflection.newProxy(rpcInterface, new MessageSendProxy()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
    }
}
