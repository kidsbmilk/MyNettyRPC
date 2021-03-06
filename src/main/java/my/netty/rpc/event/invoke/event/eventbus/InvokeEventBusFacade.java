package my.netty.rpc.event.invoke.event.eventbus;

import my.netty.rpc.jmx.invoke.ModuleMetricsHandler;

import java.util.EnumMap;
import java.util.Map;

public class InvokeEventBusFacade { // facade：外表; 建筑物的正面; 虚伪，假象;

    private static Map<AbstractInvokeEventBus.ModuleEvent, AbstractInvokeEventBus> enumMap =
            new EnumMap<>(AbstractInvokeEventBus.ModuleEvent.class);

    static {
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_EVENT, new InvokeEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_SUCC_EVENT, new InvokeSuccEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_EVENT, new InvokeFailEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_FILTER_EVENT, new InvokeFilterEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_TIMESPAM_EVENT, new InvokeTimeSpanEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_MAX_TIMESPAM_EVENT, new InvokeMaxTimeSpanEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_MIN_TIMESPAM_EVENT, new InvokeMinTimeSpanEvent());
        enumMap.put(AbstractInvokeEventBus.ModuleEvent.INVOKE_FAIL_STACKTRACE_EVENT, new InvokeFailStackTraceEvent());
    }

    public InvokeEventBusFacade(ModuleMetricsHandler handler, String moduleName, String methodName) {
        for(AbstractInvokeEventBus eventBus : enumMap.values()) {
            eventBus.setHandler(handler);
            eventBus.setModuleName(moduleName);
            eventBus.setMethodName(methodName);
        }
    }

    public AbstractInvokeEventBus fetchEvent(AbstractInvokeEventBus.ModuleEvent event) {
        if(enumMap.containsKey(event)) {
            return enumMap.get(event);
        } else {
            return null;
        }
    }
}
