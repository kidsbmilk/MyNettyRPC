package my.netty.rpc.core;

public class RpcSystemConfig {

    public static final String SystemPropertyThreadPoolRejectedPolicyAttr = "my.rpc.parallel.rejected.policy";
    public static final String SystemPropertyThreadPoolQueueNameAttr = "my.rpc.parallel.queue";
    public static final int PARALLEL = Math.max(2, Runtime.getRuntime().availableProcessors());

    private static boolean monitorServerSupport = false;

    public static boolean isMonitorServerSupport() {
        return monitorServerSupport;
    }

    public static void setMonitorServerSupport(boolean jmxSupport) {
        monitorServerSupport = jmxSupport;
    }
}
