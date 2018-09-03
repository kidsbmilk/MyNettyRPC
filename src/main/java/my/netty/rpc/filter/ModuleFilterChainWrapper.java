package my.netty.rpc.filter;

import my.netty.rpc.core.Modular;
import my.netty.rpc.core.ModuleInvoker;
import my.netty.rpc.core.ModuleProvider;
import my.netty.rpc.model.MessageRequest;

import java.util.List;

public class ModuleFilterChainWrapper implements Modular {

    private Modular modular;
    private List<ChainFilter> filters;

    public ModuleFilterChainWrapper(Modular modular) {
        if(modular == null) {
            throw new IllegalArgumentException("module is null");
        }
        this.modular = modular;
    }

    @Override
    public <T> ModuleProvider<T> invoke(ModuleInvoker<T> invoker, MessageRequest request) {
        return modular.invoke(buildChain(invoker), request);
    }

    private <T> ModuleInvoker<T> buildChain(ModuleInvoker<T> invoker) {
        ModuleInvoker now = invoker;

        if(filters.size() > 0) {
            for(int i = filters.size() - 1; i >= 0; i --) { // 注意这个顺序
                ChainFilter filter = filters.get(i);
                ModuleInvoker<T> up = now;
                now = new ModuleInvoker<T>() {
                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public Object invoke(MessageRequest request) throws Throwable {
                        return filter.invoke(up, request); // 这里把整个调用链串起来
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return now;
    }

    public List<ChainFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<ChainFilter> filters) {
        this.filters = filters;
    }
}
