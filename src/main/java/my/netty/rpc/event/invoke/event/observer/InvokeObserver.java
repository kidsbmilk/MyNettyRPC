package my.netty.rpc.event.invoke.event.observer;

import my.netty.rpc.event.invoke.event.eventbus.InvokeEventBusFacade;
import my.netty.rpc.event.invoke.event.eventbus.AbstractInvokeEventBus;
import my.netty.rpc.jmx.invoke.ModuleMetricsVisitor;

import java.util.Observable;

public class InvokeObserver extends AbstractInvokeObserver {

    public InvokeObserver(InvokeEventBusFacade facade, ModuleMetricsVisitor visitor) {
        super(facade, visitor);
    }

    /**
     * java.util.Observer类中有些方法的注释。
     * 在java.util.Observable类中也有这样一段注释：
     * An observable object can have one or more observers. An observer
     * may be any object that implements interface {@code Observer}. After an
     * observable instance changes, an application calling the
     * {@code Observable}'s {@code notifyObservers} method
     * causes all of its observers to be notified of the change by a call
     * to their {@code update} method.
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        if((AbstractInvokeEventBus.ModuleEvent) arg == AbstractInvokeEventBus.ModuleEvent.INVOKE_EVENT) {
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_EVENT)
                    .notify(super.getVisitor().getInvokeCount(), super.getVisitor().incrementInvokeCount());
        }
    }
}
