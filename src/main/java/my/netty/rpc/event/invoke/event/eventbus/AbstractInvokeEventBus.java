package my.netty.rpc.event.invoke.event.eventbus;

import my.netty.rpc.jmx.invoke.ModuleMetricsHandler;

import javax.management.Notification;

public abstract class AbstractInvokeEventBus {
    public enum ModuleEvent {
        INVOKE_EVENT,
        INVOKE_SUCC_EVENT,
        INVOKE_TIMESPAM_EVENT,
        INVOKE_MAX_TIMESPAM_EVENT,
        INVOKE_MIN_TIMESPAM_EVENT,
        INVOKE_FILTER_EVENT,
        INVOKE_FAIL_EVENT,
        INVOKE_FAIL_STACKTRACE_EVENT
    }

    protected String moduleName;
    protected String methodName;
    protected ModuleMetricsHandler handler;

    public AbstractInvokeEventBus() {
    }

    public AbstractInvokeEventBus(String moduleName, String methodName) {
        this.moduleName = moduleName;
        this.methodName = methodName;
    }

    public abstract Notification buildNotification(Object oldValue, Object newValue);

    // 见InvokeEvent.buildNotification处的注释。
    // 见AbstractInvokeObserver的类的注释。
    public void notify(Object oldValue, Object newValue) {
        Notification notification = buildNotification(oldValue, newValue); // 见javax.management.Notification类说明。
        handler.sendNotification(notification); // javax.management.NotificationBroadcasterSupport接口里的方法。
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ModuleMetricsHandler getHandler() {
        return handler;
    }

    public void setHandler(ModuleMetricsHandler handler) {
        this.handler = handler;
    }
}
