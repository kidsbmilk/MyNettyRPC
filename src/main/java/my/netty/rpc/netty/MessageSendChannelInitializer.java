package my.netty.rpc.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import my.netty.rpc.serialize.RpcSerializeProtocol;


public class MessageSendChannelInitializer extends ChannelInitializer<SocketChannel> {

    private RpcSerializeProtocol protocol;
    private RpcSendSerializeFrame frame = new RpcSendSerializeFrame();

    MessageSendChannelInitializer buildRpcSerializeProtocol(RpcSerializeProtocol protocol) {
        this.protocol = protocol;
        return this;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline(); // 对于有4次发起连接的操作，为什么这里只被调用了一次 ?zz?
        // 因为是多个线程并发执行的，而我调试时是只在一个线程上的，所以只能观察到一次，可以通过下面的代码打印出运行日志看看。
//        System.out.println("zzlog: pipeline " + Thread.currentThread());
//        System.out.println("zzlog: pipeline " + pipeline);
        frame.select(protocol, pipeline);
    }
}
