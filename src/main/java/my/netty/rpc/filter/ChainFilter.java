package my.netty.rpc.filter;

import my.netty.rpc.core.ModuleInvoker;
import my.netty.rpc.model.MessageRequest;

public interface ChainFilter {

    Object invoke(ModuleInvoker<?> invoker, MessageRequest request) throws Throwable;
}
