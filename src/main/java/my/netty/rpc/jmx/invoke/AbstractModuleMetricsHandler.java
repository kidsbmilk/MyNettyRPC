package my.netty.rpc.jmx.invoke;

import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.event.invoke.event.listener.EventNotificationListener;
import my.netty.rpc.parallel.SemaphoreWrapper;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

// 注意：这个抽象类继承自NotificationBroadcasterSupport，主要使用其两个方法，一是重写了getNotificationInfo，二是在AbstractInvokeEventBus中，
// 有一个AbstractModuleMetricsHandler的实现类ModuleMetricsHandler类型的成员变量，用到这个成员变量的sendNotification方法，这个方法就是NotificationBroadcasterSupport里的。
// JMX之Notification：https://blog.csdn.net/drykilllogic/article/details/38382797
// 从零开始玩转JMX(一)——简介和Standard MBean：https://blog.csdn.net/u013256816/article/details/52800742
// 从零开始玩转JMX(二)——Condition：https://blog.csdn.net/u013256816/article/details/52808328
// 从零开始玩转JMX(三)——Model MBean：https://blog.csdn.net/u013256816/article/details/52817247
//从零开始玩转JMX(四)——Apache Commons Modeler & Dynamic MBean：https://blog.csdn.net/u013256816/article/details/52840067
public abstract class AbstractModuleMetricsHandler extends NotificationBroadcasterSupport implements ModuleMetricsVisitorMXBean {

    protected List<ModuleMetricsVisitor> visitorList = new CopyOnWriteArrayList<ModuleMetricsVisitor>();
    protected static String startTime;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();
    private static final int METRICS_VISITOR_LIST_SIZE = HashModuleMetricsVisitor.getInstance().getHashModuleMetricsVisitorListSize();
    private MetricsTask[] tasks = new MetricsTask[METRICS_VISITOR_LIST_SIZE];
    private boolean aggregationTaskFlag = false;
    private ExecutorService executor = Executors.newFixedThreadPool(METRICS_VISITOR_LIST_SIZE);

    public ModuleMetricsVisitor getVisitor(String moduleName, String methodName) {
        try {
            enter();
            return getVisitorInCriticalSection(moduleName, methodName);
        } finally {
            exit();
        }
    }

    // 对于不同种类的Notification，应该会其每种都定义一个对应的MBeanNotificationInfo，用于描述其名称、描述等字段。
    // 见javax.management.MBeanNotificationInfo类注释。
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() { // 这个是重写了NotificationBroadcasterSupport里的方法。
        String[] types = new String[] {AttributeChangeNotification.ATTRIBUTE_CHANGE};
        String name = AttributeChangeNotification.class.getName();
        String description = "the event send from NettyRPC server!";
        MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[]{info};
    }

    public final static String getStartTime() {
        if(startTime == null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            startTime = format.format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));
        }
        return startTime;
    }

    //  这个enter()与exit()的实现与java.util.concurrent.locks.LockSupport类注释中的例子一样，参考那里的说明。
    // first-in-first-out non-reentrant lock，这个的实现很有趣，可以实现一些有趣的效果：比如多个线程按序加锁等待并按加锁顺序或者逆加锁顺序依次唤醒每个线程继续执行。
    // 通过改变线程在队列中的顺序，可以实现某些线程等待其他的线程先唤醒后自己才唤醒。这样的缺点也非常明显，多个线程阻塞，会导致系统中有大量的线程，浪费资源。
    protected void enter() {
        Thread current = Thread.currentThread();
        waiters.add(current);

        while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
            LockSupport.park(ModuleMetricsVisitor.class);
        }

        waiters.remove();
    }

    protected void exit() {
        locked.set(false);
        LockSupport.unpark(waiters.peek());
    }

    protected abstract ModuleMetricsVisitor getVisitorInCriticalSection(String moduleName, String methodName);

    @Override
    public List<ModuleMetricsVisitor> getModuleMetricsVisitor() {
        if(RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_SUPPORT) {
            CountDownLatch latch = new CountDownLatch(1);
            MetricsAggregationTask aggregationTask = new MetricsAggregationTask(aggregationTaskFlag, tasks, visitorList, latch);
            CyclicBarrier barrier = new CyclicBarrier(METRICS_VISITOR_LIST_SIZE, aggregationTask);
            for(int i = 0; i < METRICS_VISITOR_LIST_SIZE; i ++) {
                tasks[i] = new MetricsTask(barrier, HashModuleMetricsVisitor.getInstance().getHashVisitorList().get(i));
                executor.execute(tasks[i]);
            }

            try {
                visitorList.clear();
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return visitorList;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void addModuleMetricsVisitor(ModuleMetricsVisitor visitor) {
        visitorList.add(visitor);
    }
}
