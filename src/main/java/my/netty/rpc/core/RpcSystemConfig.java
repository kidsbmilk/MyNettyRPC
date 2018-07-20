package my.netty.rpc.core;

public class RpcSystemConfig {

    public static final String SystemPropertyThreadPoolRejectedPolicyAttr = "my.rpc.parallel.rejected.policy";
    public static final String SystemPropertyThreadPoolQueueNameAttr = "my.rpc.parallel.queue";
    public static final int PARALLEL = Math.max(2, Runtime.getRuntime().availableProcessors());
}
