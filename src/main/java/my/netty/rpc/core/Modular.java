package my.netty.rpc.core;

import my.netty.rpc.model.MessageRequest;

public interface Modular {

    <T> ModuleProvider<T> invoke(ModuleInvoker<T> invoker, MessageRequest request);
}
