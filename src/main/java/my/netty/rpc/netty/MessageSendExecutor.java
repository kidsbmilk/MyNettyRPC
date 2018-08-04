package my.netty.rpc.netty;

import com.google.common.reflect.Reflection;
import my.netty.rpc.serialize.RpcSerializeProtocol;

public class MessageSendExecutor {  // 这是一个单例对象。是工具类，所以设置为单例对象。

    private static class MessageSendExecutorHolder {
        private static final MessageSendExecutor instance = new MessageSendExecutor();  // 这是一个单例对象。是工具类，所以设置为单例对象。
    }

    public static MessageSendExecutor getInstance() {
        return MessageSendExecutorHolder.instance;
    }

    private RpcServerLoader loader = RpcServerLoader.getInstance();  // 这是一个单例对象。是工具类，所以设置为单例对象。

    public MessageSendExecutor() {
    }

    public MessageSendExecutor(String serverAddress, RpcSerializeProtocol serializeProtocol) {
        loader.load(serverAddress, serializeProtocol); // 这里会发起链接，并且会等待链接建立完毕。
        // 注意：是异常非阻塞的，有多个线程，每个线程上又处理多个连接。每个连接发起连接请求后，会立即返回，并不会等待连接完成，
        // 等完成后，是异步通知连接完成的，然后等待后续处理任务；而且，在发起请求到连接远程地址完成这段时间，线程并没有阻塞，
        // 而是处理此线程上其他任务了，所以线程也是非阻塞的。这也是netty实现的。
    }

    public void setRpcServerLoader(String serverAddress, RpcSerializeProtocol serializeProtocol) {
        loader.load(serverAddress, serializeProtocol); // 见上面的分析。
    }

    public void stop() {
        loader.unLoad();
    }

    public static <T> T execute(Class<T> rpcInterface) throws Exception {
        return (T) Reflection.newProxy(rpcInterface, new MessageSendProxy<T>()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
    }

    // 上面一种写法是JDK的写法，用了强制类型转换，其实下面的写法也可以，Reflection.newProxy不用强制类型转换（进而MessageSendProxy类定义中也可以去掉泛型），见PojoCallTest里的分析。
//    public static Object execute(Class rpcInterface) {
//        return Reflection.newProxy(rpcInterface, new MessageSendProxy()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
//    }
}
