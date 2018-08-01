package my.netty.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.util.Map;

public class MessageRecvHandler extends ChannelInboundHandlerAdapter {

    private final Map<String, Object> handlerMap;

    public MessageRecvHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageRequest request = (MessageRequest) msg;
        MessageResponse response = new MessageResponse();
        MessageRecvInitializeTask recvTask = new MessageRecvInitializeTask(request, response, handlerMap);
        MessageRecvExecutor.submit(recvTask, ctx, request, response);
//        System.out.println(ctx.channel().remoteAddress());
    }

    public void execptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
