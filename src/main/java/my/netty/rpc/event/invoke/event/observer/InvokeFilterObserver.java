package my.netty.rpc.event.invoke.event.observer;

import my.netty.rpc.event.invoke.event.eventbus.InvokeEventBusFacade;
import my.netty.rpc.event.invoke.event.eventbus.AbstractInvokeEventBus;
import my.netty.rpc.jmx.invoke.ModuleMetricsVisitor;

import java.util.Observable;

public class InvokeFilterObserver extends AbstractInvokeObserver {

    public InvokeFilterObserver(InvokeEventBusFacade facade, ModuleMetricsVisitor visitor) {
        super(facade, visitor);
    }

    @Override
    public void update(Observable o, Object arg) {
        if((AbstractInvokeEventBus.ModuleEvent) arg == AbstractInvokeEventBus.ModuleEvent.INVOKE_FILTER_EVENT) {
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_FILTER_EVENT)
                    .notify(super.getVisitor().getInvokeFilterCount(), super.getVisitor().incrementInvokeFilterCount());
        }
    }
}
