package my.netty.rpc.event;

import my.netty.rpc.jmx.ModuleMetricsVisitor;

import java.util.Observable;

public class InvokeFailObserver extends AbstractInvokeObserver {

    private Throwable error;

    public InvokeFailObserver(InvokeEventBusFacade facade, ModuleMetricsVisitor visitor, Throwable error) {
        super(facade, visitor);
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public void update(Observable o, Object arg) {
        if((AbstractInvokeEventBus.ModuleEvent) arg == AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_EVENT) {
            super.getFacade()
                    .fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_EVENT)
                    .notify(super.getVisitor().getInvokeFailCount(), super.getVisitor().incrementInvokeFailCount());
            super.getFacade().fetchEvent(AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_STACKTRACE_EVENT).notify(null, error);
        }
    }
}
