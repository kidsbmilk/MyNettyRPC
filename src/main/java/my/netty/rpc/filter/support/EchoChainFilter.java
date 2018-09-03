package my.netty.rpc.filter.support;

import my.netty.rpc.core.ModuleInvoker;
import my.netty.rpc.filter.ChainFilter;
import my.netty.rpc.model.MessageRequest;

public class EchoChainFilter implements ChainFilter {

    @Override
    public Object invoke(ModuleInvoker<?> invoker, MessageRequest request) throws Throwable {
        Object o = null;
        try {
            System.out.println("EchoChainFilter##TRACE MESSAGE-ID: " + request.getMessageId());
            o = invoker.invoke(request);
            return o;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        }
    }
}
