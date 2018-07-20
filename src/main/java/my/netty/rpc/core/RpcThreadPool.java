package my.netty.rpc.core;

import my.netty.rpc.parallel.NamedThreadFactory;

import java.util.concurrent.*;

public class RpcThreadPool {

    private static RejectedExecutionHandler createPolicy() {

    }

    public static Executor getExecutor(int threads, int queues) {
        String name = "RpcThreadPool";
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<Runnable>() : (queues < 0 ? new LinkedBlockingQueue<Runnable>() : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name));
    }
}
