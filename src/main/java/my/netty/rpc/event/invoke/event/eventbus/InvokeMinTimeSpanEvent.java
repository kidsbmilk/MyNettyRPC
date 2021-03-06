package my.netty.rpc.event.invoke.event.eventbus;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeMinTimeSpanEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokeMinTimeSpanNumber = new AtomicLong(0L);

    public InvokeMinTimeSpanEvent() {
        super();
    }

    public InvokeMinTimeSpanEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokeMinTimeSpanNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_MIN_TIMESPAM_EVENT.toString(), oldValue, newValue);
    }
}
