package my.netty.rpc.netty;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.event.invoke.event.eventbus.InvokeEventBusFacade;
import my.netty.rpc.event.invoke.event.observable.InvokeEventWatcher;
import my.netty.rpc.event.invoke.event.eventbus.AbstractInvokeEventBus;
import my.netty.rpc.event.invoke.event.observer.InvokeFailObserver;
import my.netty.rpc.event.invoke.event.observer.InvokeFilterObserver;
import my.netty.rpc.event.invoke.event.observer.InvokeObserver;
import my.netty.rpc.event.invoke.event.observer.InvokeSuccObserver;
import my.netty.rpc.filter.ServiceFilterBinder;
import my.netty.rpc.jmx.invoke.ModuleMetricsHandler;
import my.netty.rpc.jmx.invoke.ModuleMetricsVisitor;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import my.netty.rpc.parallel.SemaphoreWrapperFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MessageRecvInitializeTask extends AbstractMessageRecvInitializeTask {
    private AtomicReference<ModuleMetricsVisitor> visitor = new AtomicReference<ModuleMetricsVisitor>();
    private AtomicReference<InvokeEventBusFacade> facade = new AtomicReference<InvokeEventBusFacade>();
    private AtomicReference<InvokeEventWatcher> watcher = new AtomicReference<InvokeEventWatcher>(new InvokeEventWatcher());

    MessageRecvInitializeTask(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
        super(request, response, handlerMap);
    }

    @Override
    protected void injectInvoke() {
        Class cls = handlerMap.get(request.getClassName()).getClass();
        boolean binder = ServiceFilterBinder.class.isAssignableFrom(cls);
        if (binder) {
            cls = ((ServiceFilterBinder) handlerMap.get(request.getClassName())).getObject().getClass();
        }

        ReflectionUtils utils = new ReflectionUtils();

        try {
            Method method = ReflectionUtils.getDeclaredMethod(cls, request.getMethodName(), request.getTypeParameters());
            utils.listMethod(method, false);
            String signatureMethod = utils.getProvider().toString();
            visitor.set(ModuleMetricsHandler.getInstance().getVisitor(request.getClassName(), signatureMethod));
            // 上面的visitor相当于创建了与ModuleName、MethodName相对应的存储结构，用于存储调用统计信息。
            // 下面的facade是创建了一系列不同类型的与上面的visitor相关的事件。
            facade.set(new InvokeEventBusFacade(ModuleMetricsHandler.getInstance(), visitor.get().getModuleName(), visitor.get().getMethodName()));
            // 下面关联facade与visitor
            watcher.get().addObserver(new InvokeObserver(facade.get(), visitor.get())); // get()是AtomicReference的方法。
            // 下面开始触发统计流程：Observable.notifyObservers --> Observer.update --> NotificationBroadcasterSupport.sendNotification
            // --> NotificationListener.handleNotification --> 存储在visitor中。
            watcher.get().changedAndNotifyObserver(AbstractInvokeEventBus.ModuleEvent.INVOKE_EVENT);
        } finally {
            utils.clearProvider();
        }
    }

    /**
     * 观察此类中关于watcher的部分，对于每一次的调用，也就每一次发送消息，都是先加入一个监听者，然后发起事件，这样会导致监听者过多。
     * 这里应该是作者设计的有问题，应该是有限的不同种类的监听器对应无限的多样的消息。TODO-THIS.
     */
    @Override
    protected void injectSuccInvoke(long invokeTimespan) {
        watcher.get().addObserver(new InvokeSuccObserver(facade.get(), visitor.get(), invokeTimespan));
        watcher.get().changedAndNotifyObserver(AbstractInvokeEventBus.ModuleEvent.INVOKE_SUCC_EVENT);
    }

    @Override
    protected void injectFailInvoke(Throwable error) {
        watcher.get().addObserver(new InvokeFailObserver(facade.get(), visitor.get(), error));
        watcher.get().changedAndNotifyObserver(AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_EVENT);
    }

    @Override
    protected void injectFilterInvoke() {
        watcher.get().addObserver(new InvokeFilterObserver(facade.get(), visitor.get()));
        watcher.get().changedAndNotifyObserver(AbstractInvokeEventBus.ModuleEvent.INVOKE_FILTER_EVENT);
    }

    @Override
    protected void acquire() {
        SemaphoreWrapperFactory.getInstance().acquire();
    }

    @Override
    protected void release() {
        SemaphoreWrapperFactory.getInstance().release();
    }
}
