package my.netty.rpc.jmx.invoke;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MetricsAggregationTask implements Runnable {

    private boolean flag = false;
    private MetricsTask[] tasks;
    private List<ModuleMetricsVisitor> visitors;
    private CountDownLatch latch;

    public MetricsAggregationTask(boolean flag, MetricsTask[] tasks, List<ModuleMetricsVisitor> visitors, CountDownLatch latch) {
        this.flag = flag;
        this.tasks = tasks;
        this.visitors = visitors;
        this.latch = latch;
    }

    @Override
    public void run() {
        if(flag) {
            try {
                for(MetricsTask task : tasks) {
                    // System.out.println(task.getResult().get(0));
                    visitors.add(task.getResultList().get(0));
                }
            } finally {
                latch.countDown();
            }
        } else {
            flag = true;
        }
    }
}
