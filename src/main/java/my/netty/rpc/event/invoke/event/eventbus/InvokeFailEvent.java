package my.netty.rpc.event.invoke.event.eventbus;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeFailEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokeFailNumber = new AtomicLong(0L);

    public InvokeFailEvent() {
        super();
    }

    public InvokeFailEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokeFailNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_FAIL_EVENT.toString(), oldValue, newValue);
    }
}
