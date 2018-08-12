package my.netty.rpc.event;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeFilterEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokerFilterNumber = new AtomicLong(0L);

    public InvokeFilterEvent() {
        super();
    }

    public InvokeFilterEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokerFilterNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_FILTER_EVENT.toString(), oldValue, newValue);
    }
}
