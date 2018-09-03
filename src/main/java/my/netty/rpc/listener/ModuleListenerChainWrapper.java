package my.netty.rpc.listener;

import my.netty.rpc.core.Modular;
import my.netty.rpc.core.ModuleInvoker;
import my.netty.rpc.core.ModuleProvider;
import my.netty.rpc.model.MessageRequest;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class ModuleListenerChainWrapper implements Modular {

    private Modular modular;
    private List<ModuleListener> listeners;

    public ModuleListenerChainWrapper(Modular modular) {
        if(modular == null) {
            throw new IllegalArgumentException("module is null");
        }
        this.modular = modular;
    }

    @Override
    public <T> ModuleProvider<T> invoke(ModuleInvoker<T> invoker, MessageRequest request) {
        return new ModuleProviderWrapper(modular.invoke(invoker, request), Collections.unmodifiableList(listeners), request);
    }

    public List<ModuleListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<ModuleListener> listeners) {
        this.listeners = listeners;
    }
}
