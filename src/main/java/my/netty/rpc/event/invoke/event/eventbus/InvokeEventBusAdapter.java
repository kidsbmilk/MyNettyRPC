package my.netty.rpc.event.invoke.event.eventbus;

import javax.management.Notification;

public class InvokeEventBusAdapter extends AbstractInvokeEventBus {

    @Override
    public Notification buildNotification(Object oldValue, Object newValue) {
        return null;
    }
}
