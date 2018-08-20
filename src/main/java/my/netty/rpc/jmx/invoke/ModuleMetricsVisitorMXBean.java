package my.netty.rpc.jmx.invoke;

import java.util.List;

public interface ModuleMetricsVisitorMXBean {

    List<ModuleMetricsVisitor> getModuleMetricsVisitor();

    void addModuleMetricsVisitor(ModuleMetricsVisitor visitor);
}
