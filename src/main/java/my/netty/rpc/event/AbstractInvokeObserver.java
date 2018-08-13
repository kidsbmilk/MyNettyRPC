package my.netty.rpc.event;

import my.netty.rpc.jmx.ModuleMetricsVisitor;

import java.util.Observer;

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
