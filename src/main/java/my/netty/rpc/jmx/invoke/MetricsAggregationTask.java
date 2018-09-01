package my.netty.rpc.jmx.invoke;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MetricsAggregationTask implements Runnable { // 这个类的目的是用于CyclicBarrier，等所有指定的线程都就绪后执行这里的run方法。

    private boolean flag = false;
    private MetricsTask[] tasks;
    private List<ModuleMetricsVisitor> visitorList;
    private CountDownLatch latch;

    public MetricsAggregationTask(boolean flag, MetricsTask[] tasks, List<ModuleMetricsVisitor> visitorList, CountDownLatch latch) {
        this.flag = flag;
        this.tasks = tasks;
        this.visitorList = visitorList;
        this.latch = latch;
    }

    @Override
    public void run() {
        if(flag) { // 见AbstractModuleMetricsHandler.getModuleMetricsVisitorList里的注释
            try {
                for(MetricsTask task : tasks) {
                    // System.out.println(task.getResult().get(0));
                    visitorList.add(task.getResult()); // 为什么只取第一个？ TODO-THIS.
                }
            } finally {
                latch.countDown();
            }
        } else {
            flag = true;
        }
    }
}
