package my.netty.rpc.netty;

import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.util.Map;

public class MessageRecvInitializeTaskAdapter extends AbstractMessageRecvInitializeTask {

    MessageRecvInitializeTaskAdapter(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
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
