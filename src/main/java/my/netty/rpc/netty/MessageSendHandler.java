package my.netty.rpc.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import my.netty.rpc.core.MessageCallBack;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSendHandler extends ChannelInboundHandlerAdapter {

    private ConcurrentHashMap<String, MessageCallBack> mapCallBack = new ConcurrentHashMap<String, MessageCallBack>();

    private volatile Channel channel;
    private SocketAddress remoteAddr;

    public Channel getChannel() {
        return channel;
    }

    public SocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remoteAddr = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageResponse response = (MessageResponse) msg;
        String messageId = response.getMessageId();
        MessageCallBack callBack = mapCallBack.get(messageId);
        if(callBack != null) {
            mapCallBack.remove(messageId);
            callBack.over(response); // over有结束的意思，在这里的意思是：远程过程调用有结果返回了，在这里开始设置调用成功后的结果，
            // 远程调用结束了，本地得到结果后的处理还没结束。
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public MessageCallBack sendRequest(MessageRequest request) {
        MessageCallBack callBack = new MessageCallBack(request);
        mapCallBack.put(request.getMessageId(), callBack);
        channel.writeAndFlush(request);
//        System.out.println(channel.localAddress());
        return callBack;
    }
}
