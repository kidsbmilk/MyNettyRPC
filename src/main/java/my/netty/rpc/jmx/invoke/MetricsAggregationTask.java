package my.netty.rpc.jmx.invoke;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MetricsAggregationTask implements Runnable {

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
        if(flag) {
            try {
                for(MetricsTask task : tasks) {
                    // System.out.println(task.getResult().get(0));
                    visitorList.add(task.getResultList().get(0));
                }
            } finally {
                latch.countDown();
            }
        } else {
            flag = true;
        }
    }
}
