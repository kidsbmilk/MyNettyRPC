package my.netty.rpc.event.invoke.event.eventbus;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeFailStackTraceEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokeFailStackTraceNumber = new AtomicLong(0L);

    public InvokeFailStackTraceEvent() {
        super();
    }

    public InvokeFailStackTraceEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokeFailStackTraceNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_FAIL_STACKTRACE_EVENT.toString(), oldValue,newValue);
    }
}
