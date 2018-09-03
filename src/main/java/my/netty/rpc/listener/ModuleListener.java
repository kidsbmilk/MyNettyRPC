package my.netty.rpc.listener;

import my.netty.rpc.core.ModuleProvider;
import my.netty.rpc.model.MessageRequest;

public interface ModuleListener {

    void exported(ModuleProvider<?> provider, MessageRequest request);

    void unExported(ModuleProvider<?> provider, MessageRequest request);
}
