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
        ModuleInvoker last = invoker;

        if(filters.size() > 0) {
            for(int i = filters.size() - 1; i >= 0; i --) {
                ChainFilter filter = filters.get(i);
                ModuleInvoker<T> next = last;
                last = new ModuleInvoker<T>() {
                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public Object invoke(MessageRequest request) throws Throwable {
                        return filter.invoke(next, request);
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
        return last;
    }

    public List<ChainFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<ChainFilter> filters) {
        this.filters = filters;
    }
}
