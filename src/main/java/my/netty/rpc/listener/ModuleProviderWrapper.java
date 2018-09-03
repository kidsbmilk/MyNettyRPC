package my.netty.rpc.listener;

import my.netty.rpc.core.ModuleInvoker;
import my.netty.rpc.core.ModuleProvider;
import my.netty.rpc.model.MessageRequest;

import java.util.List;

public class ModuleProviderWrapper<T> implements ModuleProvider<T> {

    private ModuleProvider<T> provider;
    private MessageRequest request;
    private List<ModuleListener> listeners;

    public ModuleProviderWrapper(ModuleProvider<T> provider, List<ModuleListener> listeners, MessageRequest request) {
        if(provider == null) {
            throw new IllegalArgumentException("provider is null");
        }
        this.provider = provider;
        this.listeners = listeners;
        this.request = request;
        if(listeners != null && listeners.size() > 0) {
            RuntimeException exception = null;
            for(ModuleListener listener : listeners) {
                if(listener != null) {
                    try {
                        listener.exported(this, request);
                    } catch (RuntimeException t) {
                        exception = t;
                    }
                }
            }
            if(exception != null) {
                throw exception;
            }
        }
    }

    @Override
    public ModuleInvoker<T> getInvoker() {
        return provider.getInvoker();
    }

    @Override
    public void destroyInvoker() {
        try {
            provider.destroyInvoker();
        } finally {
            if(listeners != null && listeners.size() > 0) {
                RuntimeException exception = null;
                for(ModuleListener listener : listeners) {
                    if(listener != null) {
                        try {
                            listener.unExported(this, request);
                        } catch (RuntimeException t) {
                            exception = t;
                        }
                    }
                }
                if(exception != null) {
                    throw exception;
                }
            }
        }
    }
}
