package my.netty.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.serialize.RpcSerializeProtocol;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MessageSendInitializeTask implements Callable<Boolean> {

    private EventLoopGroup eventLoopGroup = null;
    private InetSocketAddress serverAddress = null;
    private RpcSerializeProtocol protocol;

    MessageSendInitializeTask(EventLoopGroup eventLoopGroup, InetSocketAddress serverAddress, RpcSerializeProtocol protocol) {
        this.eventLoopGroup = eventLoopGroup;
        this.serverAddress = serverAddress;
        this.protocol = protocol;
    }

    @Override
    public Boolean call() {
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(serverAddress)
                .handler(new MessageSendChannelInitializer().buildRpcSerializeProtocol(protocol));
        // 在MessageSendChannelInitializer.initChannel就会把MessageSendHandler设置到pipeline中。
        // 而下面又获取pipeline中的MessageSendHandler，然后将其设置到RpcServerLoader中，
        // 其实，在RpcServerLoader中，MessageSendHandler的作用仅仅是用来判断链接是否建立成功。

        ChannelFuture channelFuture = b.connect();
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                if(channelFuture.isSuccess()) {
                    MessageSendHandler handler = channelFuture.channel().pipeline().get(MessageSendHandler.class);
                    RpcServerLoader.getInstance().setMessageSendHandler(handler);
//                    System.out.println("zzlog: channel " + channelFuture.channel());
                } else {
                    EventLoop loop = (EventLoop) eventLoopGroup.schedule(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("NettyRPC server is down, start to reconnecting to : " + serverAddress.getAddress() + " : " + serverAddress.getPort());
                            call();
                        }
                    }, RpcSystemConfig.SYSTEM_PROPERTY_CLIENT_RECONNECT_DELAY, TimeUnit.SECONDS);
                }
            }
        });
        return Boolean.TRUE;
    }
}
