package my.netty.rpc.event;

import my.netty.rpc.jmx.ModuleMetricsVisitor;

import java.util.Observable;

public class InvokeSuccObserver extends AbstractInvokeObserver {

    private long invokerTimespan;

    public InvokeSuccObserver(InvokeEventBusFacade facade, ModuleMetricsVisitor visitor, long invokerTimespan) {
        super(facade, visitor);
        this.invokerTimespan = invokerTimespan;
    }

    public long getInvokerTimespan() {
        return invokerTimespan;
    }

    public void setInvokerTimespan(long invokerTimespan) {
        this.invokerTimespan = invokerTimespan;
    }

    @Override
    public void update(Observable o, Object arg) {
        if((AbstractInvokeEventBus.ModuleEvent) arg == AbstractInvokeEventBus.ModuleEvent.INVOKE_SUCC_EVENT) {
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_SUCC_EVENT).notify(super.getVisitor().getInvokeSuccCount(), super.getVisitor().incrementInvokeSuccCount());
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_TIMESPAM_EVENT).notify(super.getVisitor().getInvokeTimespan(), invokerTimespan);
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_MAX_TIMESPAM_EVENT).notify(super.getVisitor().getInvokeMaxTimespan(), invokerTimespan);
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_MIN_TIMESPAM_EVENT).notify(super.getVisitor().getInvokeMinTimespan(), invokerTimespan);
        }
    }
}
