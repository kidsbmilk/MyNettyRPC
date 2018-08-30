package my.netty.rpc.jmx.invoke;

import java.util.List;

// JMX的MBean的定义有约定，必须以MXBean或者MBean结尾。
// MBean与MXBean的区别：https://blog.csdn.net/expleeve/article/details/37502501
public interface ModuleMetricsVisitorMXBean {

    List<ModuleMetricsVisitor> getModuleMetricsVisitorList();

    void addModuleMetricsVisitor(ModuleMetricsVisitor visitor);
}
