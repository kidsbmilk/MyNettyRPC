package my.netty.rpc.netty;

import com.google.common.util.concurrent.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import my.netty.rpc.parallel.NamedThreadFactory;
import my.netty.rpc.core.RpcThreadPool;
import my.netty.rpc.model.MessageKeyVal;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import my.netty.rpc.serialize.RpcSerializeProtocol;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nullable;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * Rpc服务器执行模块
 */
public class MessageRecvExecutor implements ApplicationContextAware, InitializingBean {

    private String serverAddress;
    private RpcSerializeProtocol serializeProtocol = RpcSerializeProtocol.JDKSERIALIZE;

    private final static String DELIMITER = ":";
    private Map<String, Object> handlerMap = new ConcurrentHashMap<String, Object>();

    private static ListeningExecutorService threadPoolExecutor;

    public MessageRecvExecutor(String serverAddress, String serializeProtocol) {
        this.serverAddress = serverAddress;
        this.serializeProtocol = Enum.valueOf(RpcSerializeProtocol.class, serializeProtocol);
    }

    public static void submit(Callable<Boolean> task, final ChannelHandlerContext ctx, final MessageRequest request, final MessageResponse response) {
        if(threadPoolExecutor == null) {
            synchronized (MessageRecvExecutor.class) {
                if(threadPoolExecutor == null) {
                    threadPoolExecutor = MoreExecutors.listeningDecorator((ThreadPoolExecutor) RpcThreadPool.getExecutor(16, -1));
                }
            }
        }

        // 这里用的是guava并发库
        ListenableFuture<Boolean> listenableFuture = threadPoolExecutor.submit(task);
        Futures.addCallback(listenableFuture, new FutureCallback<Boolean>() {
            public void onSuccess(@Nullable Boolean result) {
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        System.out.println("RPC Server Send message-id response:" + request.getMessageId());
                        System.out.printf("%s + %s = %s\n", request.getParameters()[0], request.getParameters()[1], response.getResult());
                    }
                });
            }

            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, threadPoolExecutor);
    }

    /**
     * 这个方法在postProcessBeforeInitialization()中调用。
     * https://blog.csdn.net/xtj332/article/details/20127501
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        try {
            MessageKeyVal keyVal = (MessageKeyVal) ctx.getBean(Class.forName("my.netty.rpc.model.MessageKeyVal"));
            Map<String, Object> rpcServiceObject = keyVal.getMessageKeyVal();

            Set s = rpcServiceObject.entrySet();
            Iterator<Map.Entry<String, Object>> it = s.iterator();
            Map.Entry<String, Object> entry;

            while(it.hasNext()) {
                entry = it.next();
                handlerMap.put(entry.getKey(), entry.getValue());
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MessageRecvExecutor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 最先执行的是postProcessBeforeInitialization，然后是afterPropertiesSet，然后是init-method，然后是postProcessAfterInitialization。
     * https://blog.csdn.net/u013013553/article/details/79038702
     */
    public void afterPropertiesSet() throws Exception {
        ThreadFactory threadRpcFactory = new NamedThreadFactory("NettyRPC ThreadFactory");

        int parallel = Runtime.getRuntime().availableProcessors() * 2;

        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup(parallel, threadRpcFactory, SelectorProvider.provider());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)
                    .childHandler(new MessageRecvChannelInitializer(handlerMap).buildRpcSerializeProtocol(serializeProtocol))
                    .option(ChannelOption.SO_BACKLOG, 128)                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] ipAddr = serverAddress.split(MessageRecvExecutor.DELIMITER);

            if(ipAddr.length == 2) {
                String host = ipAddr[0];
                int port = Integer.parseInt(ipAddr[1]);
                ChannelFuture future = bootstrap.bind(host, port).sync();
                System.out.printf("Netty RPC Server start success!\nip: %s\nport: %d\nprotocol: %s\n\n", host, port, serializeProtocol);
                future.channel().closeFuture().sync();
            } else {
                System.out.printf("Netty RPC Server start fail!\n");
            }
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }
}
