package my.netty.rpc.event.invoke.event.eventbus;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeMaxTimeSpanEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokeMaxTimeSpanNumber = new AtomicLong(0L);

    public InvokeMaxTimeSpanEvent() {
        super();
    }

    public InvokeMaxTimeSpanEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokeMaxTimeSpanNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_MAX_TIMESPAM_EVENT.toString(), oldValue, newValue);
    }
}
