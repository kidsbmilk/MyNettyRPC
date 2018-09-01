package my.netty.rpc.netty;

import com.google.common.util.concurrent.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import my.netty.rpc.compiler.AccessAdaptiveProvider;
import my.netty.rpc.core.AbilityDetailProvider;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.filter.ServiceFilterBinder;
import my.netty.rpc.filter.support.SimpleFilter;
import my.netty.rpc.jmx.invoke.ModuleMetricsHandler;
import my.netty.rpc.netty.resolver.ApiEchoResolver;
import my.netty.rpc.parallel.NamedThreadFactory;
import my.netty.rpc.parallel.RpcThreadPool;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import my.netty.rpc.serialize.RpcSerializeProtocol;

import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Rpc服务器执行模块
 */
public class MessageRecvExecutor {

    private String serverAddress;
    private int echoApiPort;
    private RpcSerializeProtocol serializeProtocol = RpcSerializeProtocol.JDKSERIALIZE;

    private static final String DELIMITER = RpcSystemConfig.DELIMITER;
    private static final int PARALLEL = RpcSystemConfig.SYSTEM_PROPERTY_PARALLEL * 2;
    private static int threadNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_THREAD_NUMS;
    private static int queueNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_QUEUE_NUMS;
    private static volatile ListeningExecutorService threadPoolExecutor;
    private Map<String, Object> handlerMap = new ConcurrentHashMap<String, Object>();
    private int numberOfEchoThreadPool = 1;

    ThreadFactory threadFactory = new NamedThreadFactory("NettyRPC ThreadFactory");
    EventLoopGroup boss = new NioEventLoopGroup();
    EventLoopGroup worker = new NioEventLoopGroup(PARALLEL, threadFactory, SelectorProvider.provider());

    private MessageRecvExecutor() {
        handlerMap.clear();
        register();
    }

    private void register() {
        //        handlerMap.put(RpcSystemConfig.RPC_COMPILER_SPI_ATTR, new AccessAdaptiveProvider());
        ServiceFilterBinder binder = new ServiceFilterBinder();
        binder.setObject(new AccessAdaptiveProvider());
        binder.setFilter(new SimpleFilter());
        handlerMap.put(RpcSystemConfig.RPC_COMPILER_SPI_ATTR, binder);
        handlerMap.put(RpcSystemConfig.RPC_ABILITY_DETAIL_SPI_ATTR, new AbilityDetailProvider());
    }

    private static class MessageRecvExecutorHolder {
        static final MessageRecvExecutor INSTANCE = new MessageRecvExecutor();
    }

    public static MessageRecvExecutor getInstance() {
        return MessageRecvExecutorHolder.INSTANCE;
    }

    public static void submit(Callable<Boolean> task, final ChannelHandlerContext ctx, final MessageRequest request, final MessageResponse response) {
        if(threadPoolExecutor == null) {
            synchronized (MessageRecvExecutor.class) {
                if(threadPoolExecutor == null) {
                    threadPoolExecutor = MoreExecutors.listeningDecorator((ThreadPoolExecutor) (RpcSystemConfig.isMonitorServerSupport() ? RpcThreadPool.getExecutorWithJmx(threadNums, queueNums) : RpcThreadPool.getExecutor(threadNums, queueNums)));
                }
            }
        }

        // 这里用的是guava并发库
        // ListenableFuture in Guava
        // https://www.cnblogs.com/hupengcool/p/3991310.html
        ListenableFuture<Boolean> listenableFuture = threadPoolExecutor.submit(task);
        Futures.addCallback(listenableFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        System.out.println("RPC Server Send message-id response:" + request.getMessageId());
//                        output(request, response);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, threadPoolExecutor);
    }

    private static void output(final MessageRequest request, final MessageResponse response) {
        if(request.getMethodName().equalsIgnoreCase("add")) {
            System.out.printf("%s + %s = %s\n", request.getParametersVal()[0], request.getParametersVal()[1], response.getResult());
        }
        if(request.getMethodName().equalsIgnoreCase("multi")) {
            System.out.printf("%s * %s = %s\n", request.getParametersVal()[0], request.getParametersVal()[1], response.getResult());
        }
    }

    public void start() {
//        try { // 这个中断异常是针对下面代码中有sync()而写的，如果上面不使用sync()的话，这个可以去掉了。
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
                    .childHandler(new MessageRecvChannelInitializer(handlerMap).buildRpcSerializeProtocol(serializeProtocol))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] ipAddr = serverAddress.split(MessageRecvExecutor.DELIMITER);

            if(ipAddr.length == 2) {
                final String host = ipAddr[0];
                final int port = Integer.parseInt(ipAddr[1]);
                final ExecutorService executor = Executors.newFixedThreadPool(numberOfEchoThreadPool);
                // Java四种线程池newCachedThreadPool,newFixedThreadPool,newScheduledThreadPool,newSingleThreadExecutor
                // https://www.cnblogs.com/baizhanshi/p/5469948.html
                // Executor, ExecutorService 和 Executors 间的不同
                // https://www.cnblogs.com/gsonkeno/p/6607460.html

                ChannelFuture future = null;

//                这里作者写的非常混乱，作者把两种写法混一块了。


//                // 对于bind事件，法一：sync()后按顺序写代码，后面的代码是在主线程中执行的。
//                future = bootstrap.bind(host, port).sync(); // 这个future绑定到boss中的一个nioEventLoopGroup上。
//                // 单步调试会发现：bind操作中，启动boss的nioEventLoopGroup去绑定端口，并会创建future，然后返回这个future。
//                // sync()操作会阻塞main线程，就是等待绑定端口动作的完成。而下面的addListener，是在sync()操作完成后，也就是绑定动作完成后，
//                // 下面的addListener才会执行，执行后，将operationComplete的操作转移到boss的nioEventLoopGroup中去执行。
//                // 这也体现了，future与nioEventLoopGroup中线程绑定，一个线程处理一个连接的所有生命周期内的事件（注意，这里是监听连接）。
//                // 以前理解错了：以前以为是sync()后立即执行这个addListener了。对比:bind()和writeAndFlush()是不会阻塞当前线程的，
//                // 返回future后当前线程继续运行。
//
//                // 分析 Netty 死锁异常 BlockingOperationException
//                // http://www.linkedkeeper.com/detail/blog.action?bid=1027&utm_medium=hao.caibaojian.com&utm_source=hao.caibaojian.com
//                // 这个文章太好了！
//                // System.out.println("create future thread : " + Thread.currentThread()); // 在主线程中执行。
//                System.out.printf("Netty RPC Server start success!\nip:%s\nport:%d\nprotocol:%s\n\n", host, port, serializeProtocol);
//                ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executor);
//                completionService.submit(new ApiEchoResolver(host, echoApiPort));


                // 对于bind事件，法二：不用sync()，而是bind()后调用addListener，addListener中的代码是在boss的nioEventLoopGroup中去执行。
                future = bootstrap.bind(host, port).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
//                        System.out.println("run future Listener thread : " + Thread.currentThread()); // 在boss的nioEventLoopGroup中执行。
                        if (channelFuture.isSuccess()) {
                            System.out.printf("Netty RPC Server start success!\nip:%s\nport:%d\nprotocol:%s\nstart-time:%s\njmx-invoke-metrics:%s\n\n", host, port, serializeProtocol, ModuleMetricsHandler.getStartTime(), (RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_SUPPORT ? "open" : "close"));
                            ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executor);
                            completionService.submit(new ApiEchoResolver(host, echoApiPort));
                        }
                    }
                });

                // 在这里，两个步骤都用了异步的写法，原因见NettyRpcJdbcServerTest里的说明。


//                // 对于关闭监听连接（关闭服务器）的事件，法一：sync()后按顺序写代码，后面的代码是在主线程中执行的。
//                future.channel().closeFuture().sync();
//                executor.shutdownNow();


                // 对于关闭监听连接（关闭服务器）的事件，法二：不用sync()，而是closeFuture()后调用addListener，addListener中的代码是在boss的nioEventLoopGroup中去执行。
                future.channel().closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        executor.shutdownNow();
                    }
                });

//                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
//                service.scheduleAtFixedRate(new GetVisitorScheduledTask(), 10, 1, TimeUnit.SECONDS); // 这个执行后，主线程并不会阻塞，
                // 对比上面的sync()调用是要阻塞主线程的。
                // 注意：不要把主线程非阻塞存活、线程阻塞还有线程结束搞混了，主线程在程序运行期间不是会主动退出的，要不然整个程序就终止了，
                // 所以正常的多线程的程序在运行期间，主线程最好的状态就是非阻塞存活了。
                // 上面这个定时任务单线程启动后，主线程就是非阻塞存活的。
                // 这个开启定时获取visitor任务的操作，也可以放在ModuleMetricsHandler.start里去，可能逻辑上更清晰一些。TODO-THIS.

            } else {
                System.out.println("Netty RPC Server start fail!");
            }
//        } catch (InterruptedException e) { // 这个中断异常是针对上面代码中有sync()而写的，如果上面不使用sync()的话，这个可以去掉了。
//            e.printStackTrace();
//        }
    }

    public void stop() {
        worker.shutdownGracefully();
        boss.shutdownGracefully();
    }

    public Map<String, Object> getHandlerMap() {
        return handlerMap;
    }

    public void setHandlerMap(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RpcSerializeProtocol getSerializeProtocol() {
        return serializeProtocol;
    }

    public void setSerializeProtocol(RpcSerializeProtocol serializeProtocol) {
        this.serializeProtocol = serializeProtocol;
    }

    public int getEchoApiPort() {
        return echoApiPort;
    }

    public void setEchoApiPort(int echoApiPort) {
        this.echoApiPort = echoApiPort;
    }
}
