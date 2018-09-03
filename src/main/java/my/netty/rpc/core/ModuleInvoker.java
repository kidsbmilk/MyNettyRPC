package my.netty.rpc.core;

import my.netty.rpc.model.MessageRequest;

public interface ModuleInvoker<T> {

    Class<T> getInterface();

    Object invoke(MessageRequest request) throws Throwable;

    void destroy();
}
