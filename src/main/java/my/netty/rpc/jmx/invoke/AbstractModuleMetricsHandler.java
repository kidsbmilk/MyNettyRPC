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

    static {
        // Reduce the risk of "lost unpark" due to classloading
        Class<?> ensureLoaded = LockSupport.class;
    }

    protected List<ModuleMetricsVisitor> visitorList = new CopyOnWriteArrayList<ModuleMetricsVisitor>();
    protected static String startTime;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();
    private static final int METRICS_VISITOR_LIST_SIZE = HashModuleMetricsVisitor.getInstance().getHashModuleMetricsVisitorListsSize();
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
        boolean wasInterrupted = false;
        Thread current = Thread.currentThread();
        waiters.add(current);

        while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
            // 如果waiters的peek()值不是当前线程或者locked当前值不是false（当前值不是false，说明有其他线程执行这里把它设置为true了），则当前线程等待。
            // 反之，是当前线程且成功将locked设置为true了，则不执行下面的park。
            // 注意是while。
            LockSupport.park(ModuleMetricsVisitor.class);
            // ignore interrupts while waiting
            if (Thread.interrupted()) { // 线程醒来后，检查一下自己是否被中断过，如果被中断过，在没有满足while时，还要继续park的。
                // 注意：Thread.interrupted()与Thread.isInterrupted()方法的不同：thread.interrupted()会在检查状态后，将中断标志清空。
                // Thread.currentThread.interrupt()：https://blog.csdn.net/albertfly/article/details/52768590
                wasInterrupted = true;
            }
        }

        waiters.remove();
        // ensure correct interrupt status on return
        if (wasInterrupted) {
            Thread.currentThread().interrupt(); // 这个是重新设置线程的中断标志。
        }
        // 这个中断检查的作用是：比如有一个线程A并不知道另一个线程B的状态是什么，在A调用线程B的一个存在阻塞可能的操作后，想去中断B，
        // 但是A并不知道B此时有可能阻塞在park上，如果线程B的park部分没有检查并重设中断标志，则会导致A的中断操作不起作用。也即是，B的park阻塞要对外透明。
    }

    protected void exit() {
        locked.set(false); // 当前线程执行完退出时恢复locked为false。
        LockSupport.unpark(waiters.peek());
    }

    protected abstract ModuleMetricsVisitor getVisitorInCriticalSection(String moduleName, String methodName);

    @Override
    public List<ModuleMetricsVisitor> getModuleMetricsVisitorList() { // 这个方法是MXBean接口里的，是供jconsole远程使用的，在目前的代码中没有调用的地方。
        if(RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_SUPPORT) {
            CountDownLatch latch = new CountDownLatch(1);
            visitorList.clear(); // 这里先把visitorList清空，MetricsAggregationTask.run里又会收集MetricsTask里的ModuleMetricsVisitor到visitorList。
            // 具体收集过程是这样的：一开始MetricsAggregationTask里的flag是false，然后在MetricsTask.run()里的第一次barrier.await()时，把flag设置为true，在MetricsTask.run()里的第二次barrier.await()时，
            // 再执行MetricsAggregationTask.run就可以完成收集了。
            // 其实这里的线程与tasks里的线程是并行的，需要把clear()的操作放在前面，避免出现收集后又清空的情况出现。
            MetricsAggregationTask aggregationTask = new MetricsAggregationTask(aggregationTaskFlag, tasks, visitorList, latch);
            CyclicBarrier barrier = new CyclicBarrier(METRICS_VISITOR_LIST_SIZE, aggregationTask);
            // 当所指定的一批线程都到达barrier.await后，开始执行aggregationTask.run()
            for(int i = 0; i < METRICS_VISITOR_LIST_SIZE; i ++) { // 这个METRICS_VISITOR_LIST_SIZE是：比如有5个接口类，每个接口有3个方法，那么这个METRICS_VISITOR_LIST_SIZE值为5 * 3，
                // 而这15个元素都是List<ModuleMetricsVisitor>类型的。具体见HashModuleMetricsVisitor里的代码。
                tasks[i] = new MetricsTask(barrier, HashModuleMetricsVisitor.getInstance().getHashVisitorLists().get(i));
                executor.execute(tasks[i]);
            }

            try {
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
