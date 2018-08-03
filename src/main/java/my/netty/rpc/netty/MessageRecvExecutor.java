package my.netty.rpc.netty;

import com.google.common.util.concurrent.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import my.netty.rpc.compiler.AccessAdaptiveProvider;
import my.netty.rpc.core.AbilityDetailProvider;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.netty.resolver.ApiEchoResolver;
import my.netty.rpc.parallel.NamedThreadFactory;
import my.netty.rpc.parallel.RpcThreadPool;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import my.netty.rpc.serialize.RpcSerializeProtocol;

import javax.annotation.Nullable;
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
    private int parallel = RpcSystemConfig.PARALLEL * 2;
    private static int threadNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_THREAD_NUMS;
    private static int queueNums = RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_QUEUE_NUMS;
    private static volatile ListeningExecutorService threadPoolExecutor;
    private Map<String, Object> handlerMap = new ConcurrentHashMap<String, Object>();
    private int numberOfEchoThreadPool = 1;

    ThreadFactory threadFactory = new NamedThreadFactory("NettyRPC ThreadFactory");
    EventLoopGroup boss = new NioEventLoopGroup();
    EventLoopGroup worker = new NioEventLoopGroup(parallel, threadFactory, SelectorProvider.provider());

    public MessageRecvExecutor() {
        handlerMap.clear();
        register();
    }

    private static class MessageRecvExecutorHolder {
        static final MessageRecvExecutor instance = new MessageRecvExecutor();
    }

    public static MessageRecvExecutor getInstance() {
        return MessageRecvExecutorHolder.instance;
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
            public void onSuccess(@Nullable Boolean result) {
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        System.out.println("RPC Server Send message-id response:" + request.getMessageId());
//                        output(request, response);
                    }
                });
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, threadPoolExecutor);
    }

    private static void output(final MessageRequest request, final MessageResponse response) {
        if(request.getMethodName().equalsIgnoreCase("add")) {
            System.out.printf("%s + %s = %s\n", request.getParameters()[0], request.getParameters()[1], response.getResult());
        }
        if(request.getMethodName().equalsIgnoreCase("multi")) {
            System.out.printf("%s * %s = %s\n", request.getParameters()[0], request.getParameters()[1], response.getResult());
        }
    }

    public void start() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
                    .childHandler(new MessageRecvChannelInitializer(handlerMap).buildRpcSerializeProtocol(serializeProtocol))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] ipAddr = serverAddress.split(MessageRecvExecutor.DELIMITER);

            if(ipAddr.length == 2) {
                String host = ipAddr[0];
                int port = Integer.parseInt(ipAddr[1]);
                ChannelFuture future = null;
                future = bootstrap.bind(host, port).sync();
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            ExecutorService executor = Executors.newFixedThreadPool(numberOfEchoThreadPool);
                            ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executor);
                            completionService.submit(new ApiEchoResolver(host, echoApiPort));
                            System.out.printf("Netty RPC Server start success!\nip:%s\nport:%d\nprotocol:%s\n\n", host, port, serializeProtocol);
                            channelFuture.channel().closeFuture().sync().addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) throws Exception {
                                    executor.shutdownNow();
                                }
                            });
                        }
                    }
                });
            } else {
                System.out.println("Netty RPC Server start fail!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        worker.shutdownGracefully();
        boss.shutdownGracefully();
    }
	
	private void register() {
        handlerMap.put(RpcSystemConfig.RPC_COMPILER_SPI_ATTR, new AccessAdaptiveProvider());
        handlerMap.put(RpcSystemConfig.RPC_ABILITY_DETAIL_SPI_ATTR, new AbilityDetailProvider());
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
