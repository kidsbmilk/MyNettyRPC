package my.netty.rpc.event;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import java.util.concurrent.atomic.AtomicLong;

public class InvokeEvent extends AbstractInvokeEventBus {

    private AtomicLong sequenceInvokeNumber = new AtomicLong(0L);

    public InvokeEvent() {
        super();
    }

    public InvokeEvent(String moduleName, String methodName) {
        super(moduleName, methodName);
    }

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return new AttributeChangeNotification(this, sequenceInvokeNumber.incrementAndGet(), System.currentTimeMillis(),
                super.moduleName, super.methodName, ModuleEvent.INVOKE_EVENT.toString(), oldValue, newValue);
        // 见javax.management.AttributeChangeNotification的类说明，如果一个MBean对象想在其属性变化时通知监听者，
        // 则需要创建AttributeChangeNotification并发射此对象。
        // 这里是创建此对象，属性为super.methodName，类型为ModuleEvent.INVOKE_EVENT.toString()，
        // 发射此对象的代码是在AbstractInvokeEventBus.notify里，ModuleMetricsHandler是一个MBean对象，见ModuleMetricsHandler类的说明。

        // 见AbstractInvokeObserver的类的注释。
    }
}
