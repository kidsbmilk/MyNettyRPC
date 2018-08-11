package my.netty.rpc.netty;

import com.google.common.util.concurrent.*;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.parallel.RpcThreadPool;
import my.netty.rpc.serialize.RpcSerializeProtocol;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RpcServerLoader {

    private static volatile RpcServerLoader rpcServerLoader; // 这是一个单例对象。是工具类，所以设置为单例对象。
    private static final String DELIMITER = RpcSystemConfig.DELIMITER;
    private RpcSerializeProtocol serializeProtocol = RpcSerializeProtocol.JDKSERIALIZE;

    private static final int parallel = RpcSystemConfig.PARALLEL * 2;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(parallel);
    private static int threadNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_THREAD_NUMS;
    private static int queueNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_QUEUE_NUMS;
    private static ListeningExecutorService threadPoolExecutor = MoreExecutors.listeningDecorator((ThreadPoolExecutor) RpcThreadPool.getExecutor(threadNums, queueNums));
    // ListenableFuture in Guava
    // https://www.cnblogs.com/hupengcool/p/3991310.html

    private MessageSendHandler messageSendHandler = null;

    private Lock lock = new ReentrantLock();
    private Condition connectStatus = lock.newCondition();
    private Condition handlerStatus = lock.newCondition();

    private RpcServerLoader() {
    }

    public static RpcServerLoader getInstance() {
        if(rpcServerLoader == null) {
            synchronized (RpcServerLoader.class) {
                if(rpcServerLoader == null) {
                    rpcServerLoader = new RpcServerLoader();
                }
            }
        }
        return rpcServerLoader; // 这是一个单例对象。是工具类，所以设置为单例对象。
    }

    public void load(String serverAddress, RpcSerializeProtocol serializeProtocol) {
        String[] ipAddr = serverAddress.split(RpcServerLoader.DELIMITER);
        if(ipAddr.length == 2) {
            String host = ipAddr[0];
            int port = Integer.parseInt(ipAddr[1]);
            final InetSocketAddress remoteAddr = new InetSocketAddress(host, port);

            // 这里用的是guava并发库
            // ListenableFuture in Guava
            // https://www.cnblogs.com/hupengcool/p/3991310.html
            ListenableFuture<Boolean> listenableFuture = threadPoolExecutor.submit(new MessageSendInitializeTask(eventLoopGroup, remoteAddr, serializeProtocol));
            // 注意：这个threadPoolExecutor仅仅是用于创建返回listenableFuture的任务并执行，而连接始终是绑定到eventLoopGroup中的线程上的。
            // 注意：RpcServerLoader是一个单例模式，是一个发起连接的工具类，而这个类中，有成员变量eventLoopGroup，具体发起请求的类是MessageSendInitializeTask，
            // 对于多个客户端连接发起请求，会共用一个eventLoopGroup，这样就成一个连接池了，有点数据库连接池的感觉。

            // 在MessageSendInitializeTask中会设置messageSendHandler
            Futures.addCallback(listenableFuture, new FutureCallback<Boolean>() {
                public void onSuccess(Boolean result) {
                    try {
                        lock.lock();

                        if(messageSendHandler == null) {
                            handlerStatus.await(); // 这个跟setMessageSendHandler里的handlerStatus.signal()对应。
                        }

                        if(result == Boolean.TRUE && messageSendHandler != null) {
                            connectStatus.signalAll(); // 这个跟getMessageSendHandler里的connectStatus.await()相对应。
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RpcServerLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        lock.unlock();
                    }
                }

                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            }, threadPoolExecutor);
        }
    }

    public void setMessageSendHandler(MessageSendHandler messageSendHandler) {
        try {
            lock.lock();
            this.messageSendHandler = messageSendHandler;
            handlerStatus.signal();
        } finally {
            lock.unlock();
        }
    }

    public MessageSendHandler getMessageSendHandler() throws InterruptedException {
        try {
            lock.lock(); // 这个只所以要加锁，是因为有多个线程要使用messageSendHandler去发送信息，而发起连接的线程要设置这个messageSendHandler，所以要加锁，注意是个可重入锁。
            // 对比：在RpcParallel中会调用MultiCalcParallelRequestThread，进而调用MessageSendProxy.handleInvocation来并发的发请求，而在那里，就没有对handler.sendRequest加锁。
            // 原因在于每个netty连接都是绑定到一个eventLoopGroup中的线程上的，始终由这个线程来处理这个连接上的请求，所以，并不需要加锁琰处理channel.writeAndFlush。
            // Netty : writeAndFlush的线程安全及并发问题: https://blog.csdn.net/binhualiu1983/article/details/51646160
            if(messageSendHandler == null) {
                connectStatus.await();
            }
            return messageSendHandler;
        } finally {
            lock.unlock();
        }
    }

    public void unLoad() {
        messageSendHandler.close();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    public void setSerializeProtocol(RpcSerializeProtocol serializeProtocol) {
        this.serializeProtocol = serializeProtocol;
    }
}
