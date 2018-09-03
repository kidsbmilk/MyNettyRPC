package my.netty.rpc.listener.support;

import my.netty.rpc.core.ModuleProvider;
import my.netty.rpc.listener.ModuleListener;
import my.netty.rpc.model.MessageRequest;
import org.apache.commons.lang3.StringUtils;

public class ModuleListenerAdapter implements ModuleListener {

    @Override
    public void exported(ModuleProvider<?> provider, MessageRequest request) {
        System.out.println(StringUtils.center("[ModuleListenerAdapter##exported]", 48, "*"));
    }

    @Override
    public void unExported(ModuleProvider<?> provider, MessageRequest request) {
        System.out.println(StringUtils.center("[ModuleListenerAdapter##unExported]", 48, "*"));
    }
}
