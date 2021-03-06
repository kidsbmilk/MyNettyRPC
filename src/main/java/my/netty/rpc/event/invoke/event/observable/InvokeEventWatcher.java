package my.netty.rpc.event.invoke.event.observable;

import my.netty.rpc.event.invoke.event.eventbus.AbstractInvokeEventBus;

import java.util.Observable;

public class InvokeEventWatcher extends Observable {

    public void changedAndNotifyObserver(AbstractInvokeEventBus.ModuleEvent event) { // 这个方法名起的不好，此方法的作用是设置所观察对象的状态已改变，然后发起通知观察者的操作。
        // 将方法名由原来的watch改为changedAndNotify
        setChanged();
        notifyObservers(event); // 通知调用Observer的update方法，见AbstractInvokeObserver的类注释。
    }
}
