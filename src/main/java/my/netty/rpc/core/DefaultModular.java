package my.netty.rpc.core;

import my.netty.rpc.model.MessageRequest;

public class DefaultModular implements Modular {

    @Override
    public <T> ModuleProvider<T> invoke(ModuleInvoker<T> invoker, MessageRequest request) {
        return new ModuleProvider<T>() {
            @Override
            public ModuleInvoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void destroyInvoker() {
                invoker.destroy();
            }
        };
    }
}
