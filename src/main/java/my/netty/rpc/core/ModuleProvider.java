package my.netty.rpc.core;

public interface ModuleProvider<T> {

    ModuleInvoker<T> getInvoker();

    void destroyInvoker();
}
