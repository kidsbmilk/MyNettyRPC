package my.netty.rpc.jmx;

import my.netty.rpc.parallel.SemaphoreWrapper;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractModuleMetricsHandler extends NotificationBroadcasterSupport implements ModuleMetricsVisitorMXBean {

    public final static String MBEAN_NAME = "my.netty.rpc:type=ModuleMetricsHandler"; // 在JMX的MBean树里可以找到这个对象
    public final static int MODULE_METRICS_JMX_PORT = 1098;
    protected String moduleMetricsJmxUrl = "";
    protected Semaphore semaphore = new Semaphore(0);
    protected SemaphoreWrapper semaphoreWrapper = new SemaphoreWrapper(semaphore);
    protected List<ModuleMetricsVisitor> visitorList = new CopyOnWriteArrayList<ModuleMetricsVisitor>();
    protected static String startTime;
    protected ModuleMetricsListener listener = new ModuleMetricsListener(); // 这个用于更新ModuleMetricsVisitor里的数据
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

    public ModuleMetricsVisitor visit(String moduleName, String methodName) {
        try {
            enter();
            return visitCriticalSection(moduleName, methodName);
        } finally {
            exit();
        }
    }

    @Override
    public List<ModuleMetricsVisitor> getModuleMetricsVisitor() {
        return visitorList;
    }

    @Override
    public void addModuleMetricsVisitor(ModuleMetricsVisitor visitor) {
        visitorList.add(visitor);
    }

    // 对于不同种类的Notification，应该会其每种都定义一个对应的MBeanNotificationInfo，用于描述其名称、描述等字段。
    // 见javax.management.MBeanNotificationInfo类注释。
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
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

    protected abstract ModuleMetricsVisitor visitCriticalSection(String moduleName, String methodName);
}
