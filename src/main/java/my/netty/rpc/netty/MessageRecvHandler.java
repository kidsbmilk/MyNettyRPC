package my.netty.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.util.Map;
import java.util.concurrent.Callable;

public class MessageRecvHandler extends ChannelInboundHandlerAdapter {

    private final Map<String, Object> handlerMap;

    public MessageRecvHandler(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageRequest request = (MessageRequest) msg;
        MessageResponse response = new MessageResponse();
//        MessageRecvInitializeTask recvTask = new MessageRecvInitializeTask(request, response, handlerMap);
        boolean isMetrics = (RpcSystemConfig.SYSTEM_PROPERTY_JMX_INVOKE_METRICS != 0);
        Callable<Boolean> recvTask = isMetrics ?
                new MessageRecvInitializeTask(request, response, handlerMap) :
                new MessageRecvInitializeTaskAdapter(request, response, handlerMap); // 几个父类中的抽象方法的实现为空。
        MessageRecvExecutor.submit(recvTask, ctx, request, response);
//        System.out.println(ctx.channel().remoteAddress());
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
