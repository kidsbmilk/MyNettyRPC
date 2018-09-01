package my.netty.rpc.jmx.invoke;

import java.util.List;

// JMX的MBean的定义有约定，必须以MXBean或者MBean结尾。
// MBean与MXBean的区别：https://blog.csdn.net/expleeve/article/details/37502501
public interface ModuleMetricsVisitorMXBean {

    List<ModuleMetricsVisitor> getModuleMetricsVisitorList(); // 这个接口在程序里没有用到，是给jconsole使用的，但是，
    // 它返回的是一个对象的列表，展示在属性里，通过表格式数据导航的形式展示出来，而不是将此接口展示在操作里。
    // 我感觉，表格式数据导航的刷新按钮操作，也是调用了这个接口方法。
    // 我的感觉是正确的，可以把这个接口注释掉，观察一下jconsole里的展示情况。

    void addModuleMetricsVisitor(ModuleMetricsVisitor visitor);
}
