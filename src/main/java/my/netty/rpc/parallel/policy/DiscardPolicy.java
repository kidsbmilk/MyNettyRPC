package my.netty.rpc.parallel.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class DiscardPolicy implements RejectedExecutionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DiscardPolicy.class);

    private String threadName;

    public DiscardPolicy() {
        this(null);
    }

    public DiscardPolicy(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if(threadName != null) {
            LOG.error("RPC Thread pool [{}] is exhausted, executor = {}", threadName, executor.toString());
        }

        if(!executor.isShutdown()) {
            BlockingQueue<Runnable> queue = executor.getQueue();
            int discardSize = queue.size() >> 1;
            for(int i = 0; i < discardSize; i ++) {
                queue.poll();
            }

            queue.offer(runnable);
        }
    }
}
