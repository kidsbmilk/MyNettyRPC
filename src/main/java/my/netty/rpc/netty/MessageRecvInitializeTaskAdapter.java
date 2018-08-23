package my.netty.rpc.netty;

import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.util.Map;

// 几个父类中的抽象方法的实现为空
public class MessageRecvInitializeTaskAdapter extends AbstractMessageRecvInitializeTask {

    public MessageRecvInitializeTaskAdapter(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
        super(request, response, handlerMap);
    }

    @Override
    protected void injectInvoke() {
    }

    @Override
    protected void injectSuccInvoke(long invokeTimespan) {
    }

    @Override
    protected void injectFailInvoke(Throwable error) {
    }

    @Override
    protected void injectFilterInvoke() {
    }

    @Override
    protected void acquire() {
    }

    @Override
    protected void release() {
    }
}
