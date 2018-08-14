package my.netty.rpc.event;

import my.netty.rpc.jmx.ModuleMetricsVisitor;

import java.util.Observer;

/**
 * Observer与Observable是一对，要配套使用，在这里InvokeEventWatcher继承了Observable，在MessageRecvInitializeTask里用到了Observable，将Observer与Observable关联。
 *
 * Notification用于JMX监控中的消息通知，与上面是不一样的，ModuleMetricsHandler.start中有添加Notification的listener的操作：addNotificationListener。
 * ModuleMetricsListener实现了NotificationListener接口。
 * 在NotificationBroadcasterSupport的类说明里，可以看到一些关键信息：
 1、当一个线程调用sendNotification时，每个监听者的NotificationListener.handleNotification操作将在此线程中执行，这样的操作是同步的。可以通过扩展子类，重写这个函数，通过传递一个thread变量来做到异步执行NotificationListener.handleNotification的目的。
 2、有关于filter或者listener抛出Exception与Error时，类的默认处理情况
 3、远程的监听者在得到消息通知时的调用并不是同步的，当一个消息发出时，并不能保证远程的监听者一定能收到此消息。
 上述这些可以做为可改进的地方。TODO-THIS.
 */
public abstract class AbstractInvokeObserver implements Observer { // 这个Observer接口将被移除了。见Observable接口里的说明，使用java.beans改写这里的代码。TODO-THIS.

    private InvokeEventBusFacade facade;
    private ModuleMetricsVisitor visitor;

    public AbstractInvokeObserver(InvokeEventBusFacade facade, ModuleMetricsVisitor visitor) {
        this.facade = facade;
        this.visitor = visitor;
    }

    public InvokeEventBusFacade getFacade() {
        return facade;
    }

    public void setFacade(InvokeEventBusFacade facade) {
        this.facade = facade;
    }

    public ModuleMetricsVisitor getVisitor() {
        return visitor;
    }

    public void setVisitor(ModuleMetricsVisitor visitor) {
        this.visitor = visitor;
    }
}
