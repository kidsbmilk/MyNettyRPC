package my.netty.rpc.jmx.invoke;

import org.apache.commons.collections.iterators.UniqueFilterIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MetricsTask implements Runnable {

    private final CyclicBarrier barrier;
    private List<ModuleMetricsVisitor> visitorList;
    private ModuleMetricsVisitor result;

    public MetricsTask(CyclicBarrier barrier, List<ModuleMetricsVisitor> visitorList) {
        this.barrier = barrier;
        this.visitorList = visitorList;
    }

    @Override
    public void run() {
        try {
            barrier.await();
            accumulate(); // 收集数据，注意，这里是并行收集的，每个线程的visitorList并不一样，见AbstractModuleMetricsHandler.getModuleMetricsVisitorList里的设置。
            barrier.await(); // 见AbstractModuleMetricsHandler.getModuleMetricsVisitorList里的注释。
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    private void count(List<ModuleMetricsVisitor> list) { // 见accumulate里的说明，理论上：result.size() <= list.size().
        long invokeCount = 0L;
        long invokeSuccCount = 0L;
        long invokeFailCount = 0L;
        long invokeFilterCount = 0L;
        long invokeTimespan = 0L;
        long invokeMinTimespan = list.get(0).getInvokeMinTimespan();
        long invokeMaxTimespan = list.get(0).getInvokeMaxTimespan();
        int length = result.getHistogram().getRanges().length + 1;
        long[] invokeHistogram = new long[length];
        Arrays.fill(invokeHistogram, 0L);
        String lastStackTraceDetail = "";
        long lastErrorTime = list.get(0).getErrorLastTimeLongVal();

        ModuleMetrics metrics = new ModuleMetrics();
        metrics.setInvokeCount(invokeCount);
        metrics.setInvokeSuccCount(invokeSuccCount);
        metrics.setInvokeFailCount(invokeFailCount);
        metrics.setInvokeFilterCount(invokeFilterCount);
        metrics.setInvokeTimespan(invokeTimespan);
        metrics.setInvokeMinTimespan(invokeMinTimespan);
        metrics.setInvokeMaxTimespan(invokeMaxTimespan);
        metrics.setInvokeHistogram(invokeHistogram);
        metrics.setLastStackTraceDetail(lastStackTraceDetail);
        metrics.setLastErrorTime(lastErrorTime);

        merge(list, metrics);

        result.setInvokeCount(metrics.getInvokeCount());
        result.setInvokeSuccCount(metrics.getInvokeSuccCount());
        result.setInvokeFailCount(metrics.getInvokeFailCount());
        result.setInvokeFilterCount(metrics.getInvokeFilterCount());
        result.setInvokeTimespan(metrics.getInvokeTimespan());
        result.setInvokeMaxTimespan(metrics.getInvokeMaxTimespan());
        result.setInvokeMinTimespan(metrics.getInvokeMinTimespan());
        result.setInvokeHistogram(metrics.getInvokeHistogram());

        if(metrics.getLastErrorTime() > 0) {
            result.setErrorLastTimeLongVal(metrics.getLastErrorTime());
            result.setLastStackTraceDetail(metrics.getLastStackTraceDetail());
        }
    }

    private void merge(List<ModuleMetricsVisitor> list, ModuleMetrics metrics) {
        long invokeCount = metrics.getInvokeCount();
        long invokeSuccCount = metrics.getInvokeSuccCount();
        long invokeFailCount = metrics.getInvokeFailCount();
        long invokeFilterCount = metrics.getInvokeFilterCount();
        long invokeTimespan = metrics.getInvokeTimespan();
        long invokeMinTimespan = metrics.getInvokeMinTimespan();
        long invokeMaxTimespan = metrics.getInvokeMaxTimespan();
        long[] invokeHistogram = metrics.getInvokeHistogram();
        String lastStackTraceDetail = metrics.getLastStackTraceDetail();
        long lastErrorTime = metrics.getLastErrorTime();

        for(int i = 0; i < list.size(); i ++) { // 过滤list里的元素，比较每个元素是否与resut.get(index)相同。
            boolean find = equals(result.getModuleName(), list.get(i).getModuleName(), result.getMethodName(), list.get(i).getMethodName());
            if(find) {
                invokeCount += list.get(i).getInvokeCount();
                invokeSuccCount += list.get(i).getInvokeSuccCount();
                invokeFailCount += list.get(i).getInvokeFailCount();
                invokeFilterCount += list.get(i).getInvokeFilterCount();
                long timespan = list.get(i).getInvokeTimespan();
                if (timespan > 0) {
                    invokeTimespan = timespan;
                }
                long minTimespan = list.get(i).getInvokeMinTimespan();
                long maxTimespan = list.get(i).getInvokeMaxTimespan();
                if (minTimespan < invokeMinTimespan) {
                    invokeMinTimespan = minTimespan;
                }
                if (maxTimespan > invokeMaxTimespan) {
                    invokeMaxTimespan = maxTimespan;
                }
                for (int j = 0; j < invokeHistogram.length; j++) {
                    invokeHistogram[j] += list.get(i).getHistogram().toArray()[j];
                }

                long fail = list.get(i).getInvokeFailCount();
                if (fail > 0) {
                    long lastTime = list.get(i).getErrorLastTimeLongVal();
                    if (lastTime > lastErrorTime) {
                        lastErrorTime = lastTime;
                        lastStackTraceDetail = list.get(i).getLastStackTraceDetail();
                    }
                }
            }
        }

        metrics.setInvokeCount(invokeCount);
        metrics.setInvokeSuccCount(invokeSuccCount);
        metrics.setInvokeFailCount(invokeFailCount);
        metrics.setInvokeFilterCount(invokeFilterCount);
        metrics.setInvokeTimespan(invokeTimespan);
        metrics.setInvokeMinTimespan(invokeMinTimespan);
        metrics.setInvokeMaxTimespan(invokeMaxTimespan);
        metrics.setInvokeHistogram(invokeHistogram);
        metrics.setLastStackTraceDetail(lastStackTraceDetail);
        metrics.setLastErrorTime(lastErrorTime);
    }

    private void accumulate() {
        List<ModuleMetricsVisitor> list = visitorList;

        Iterator iterator = new UniqueFilterIterator(list.iterator());
        if(iterator.hasNext()) {
            ModuleMetricsVisitor visitor = (ModuleMetricsVisitor) iterator.next();
            result = new ModuleMetricsVisitor(visitor.getModuleName(), visitor.getMethodName()); // result里的对象没有重复的，理论上：result.size() <= list.size()
        } // 这里暂时不用考虑空的情况，因为不会为空。TODO-THIS.

        count(list);
    }

    private boolean equals(String srcModuleName, String destModuleName, String srcMethodName, String destMethodName) {
        return srcModuleName.equals(destModuleName) && srcMethodName.equals(destMethodName);
    }

    public ModuleMetricsVisitor getResult() {
        return result;
    }

    public void setResult(ModuleMetricsVisitor result) {
        this.result = result;
    }

    private class ModuleMetrics {
        private long invokeCount;
        private long invokeSuccCount;
        private long invokeFailCount;
        private long invokeFilterCount;
        private long invokeTimespan;
        private long invokeMinTimespan;
        private long invokeMaxTimespan;
        private long[] invokeHistogram;
        private String lastStackTraceDetail;
        private long lastErrorTime;

        public long getInvokeCount() {
            return invokeCount;
        }

        public void setInvokeCount(long invokeCount) {
            this.invokeCount = invokeCount;
        }

        public long getInvokeSuccCount() {
            return invokeSuccCount;
        }

        public void setInvokeSuccCount(long invokeSuccCount) {
            this.invokeSuccCount = invokeSuccCount;
        }

        public long getInvokeFailCount() {
            return invokeFailCount;
        }

        public void setInvokeFailCount(long invokeFailCount) {
            this.invokeFailCount = invokeFailCount;
        }

        public long getInvokeFilterCount() {
            return invokeFilterCount;
        }

        public void setInvokeFilterCount(long invokeFilterCount) {
            this.invokeFilterCount = invokeFilterCount;
        }

        public long getInvokeTimespan() {
            return invokeTimespan;
        }

        public void setInvokeTimespan(long invokeTimespan) {
            this.invokeTimespan = invokeTimespan;
        }

        public long getInvokeMinTimespan() {
            return invokeMinTimespan;
        }

        public void setInvokeMinTimespan(long invokeMinTimespan) {
            this.invokeMinTimespan = invokeMinTimespan;
        }

        public long getInvokeMaxTimespan() {
            return invokeMaxTimespan;
        }

        public void setInvokeMaxTimespan(long invokeMaxTimespan) {
            this.invokeMaxTimespan = invokeMaxTimespan;
        }

        public long[] getInvokeHistogram() {
            return invokeHistogram;
        }

        public void setInvokeHistogram(long[] invokeHistogram) {
            this.invokeHistogram = invokeHistogram;
        }

        public String getLastStackTraceDetail() {
            return lastStackTraceDetail;
        }

        public void setLastStackTraceDetail(String lastStackTraceDetail) {
            this.lastStackTraceDetail = lastStackTraceDetail;
        }

        public long getLastErrorTime() {
            return lastErrorTime;
        }

        public void setLastErrorTime(long lastErrorTime) {
            this.lastErrorTime = lastErrorTime;
        }
    }
}
