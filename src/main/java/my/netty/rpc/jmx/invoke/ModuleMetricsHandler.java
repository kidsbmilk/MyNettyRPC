package my.netty.rpc.jmx.invoke;

import my.netty.rpc.netty.MessageRecvExecutor;
import my.netty.rpc.parallel.AbstractDaemonThread;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.lang3.StringUtils;

import javax.management.*;
import javax.management.remote.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.Iterator;

import static my.netty.rpc.core.RpcSystemConfig.DELIMITER;

/**
 * 此类对象是一个MBean，在此类的继承体系上，有一个类javax.management.NotificationBroadcasterSupport，其类注释上有这样一句话：
 * This can be used as the super class of an MBean that sends notifications.
 */
public class ModuleMetricsHandler extends AbstractModuleMetricsHandler {

    private static final ModuleMetricsHandler INSTANCE = new ModuleMetricsHandler(); // 单例模式
    private MBeanServerConnection connection;

    public static ModuleMetricsHandler getInstance() {
        return INSTANCE;
    }

    private ModuleMetricsHandler() {
    }

    @Override
    protected ModuleMetricsVisitor getVisitorInCriticalSection(String moduleName, String methodName) {
        final String method = methodName.trim();
        final String module = moduleName.trim();

        // FIXME: JMX度量临界区要注意线程间的并发竞争，否则会统计数据失真
        // 上面这个FIXME是指，如果存在线程间的竞争，会使统计数据失真，作者这里为了避免统计数据失真，在临界区里来避免竞争了，
        // 但是也导致MessageRecvInitializeTask.injectInvoke里的异步调用并行改串行的问题，见那里的注释。

        // 见FilterIterator类的注释，这个类将封装一个iterator，只有满足predicate.evaluate条件时才返回一个对象。
        Iterator iterator = new FilterIterator(visitorList.iterator(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                String startModuleName = ((ModuleMetricsVisitor) object).getModuleName();
                String startMethodName = ((ModuleMetricsVisitor) object).getMethodName();
                return startModuleName.compareTo(module) == 0 && startMethodName.compareTo(method) == 0;
            }
        });

        ModuleMetricsVisitor visitor = null;
        while(iterator.hasNext()) {
            visitor = (ModuleMetricsVisitor) iterator.next();
            break;
        }

        if(visitor != null) {
            return visitor;
        } else {
            visitor = new ModuleMetricsVisitor(module, method);
            addModuleMetricsVisitor(visitor);
            return visitor;
        }
    }

    public void start() { // 在NettyRpcRegister.afterPropertiesSet中被调用了。
        new AbstractDaemonThread() {
            @Override
            public String getDaemonThreadName() {
                return ModuleMetricsHandler.class.getSimpleName();
            }

            @Override
            public void run() {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                try {
                    LocateRegistry.createRegistry(MODULE_METRICS_JMX_PORT);
                    MessageRecvExecutor ref = MessageRecvExecutor.getInstance();
                    String ipAddr = StringUtils.isNotEmpty(ref.getServerAddress()) ? StringUtils.substringBeforeLast(ref.getServerAddress(), DELIMITER) : "localhost";
                    moduleMetricsJmxUrl = "service:jmx:rmi:///jndi/rmi://" + ipAddr + ":" + MODULE_METRICS_JMX_PORT + "/NettyRPCServer";
                    JMXServiceURL url = new JMXServiceURL(moduleMetricsJmxUrl);
                    JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);

                    ObjectName name = new ObjectName(MBEAN_NAME);

                    mbs.registerMBean(ModuleMetricsHandler.this, name);
                    mbs.addNotificationListener(name, listener, null, null);
                    cs.start();

                    semaphoreWrapper.release();

                    System.out.printf("NettyRPC JMX server is start success!\njmx-url:[ %s ]\n\n", moduleMetricsJmxUrl);
                } catch (IOException | MBeanRegistrationException | InstanceAlreadyExistsException | NotCompliantMBeanException |
                        MalformedObjectNameException | InstanceNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void stop() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName name = new ObjectName(MBEAN_NAME);
            mbs.unregisterMBean(name);
        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanRegistrationException e) {
            e.printStackTrace();
        }
    }

    public MBeanServerConnection connect() {
        try {
            if(!semaphoreWrapper.isReleased()) {
                semaphoreWrapper.acquire(); // 等待，直到JMXConnectorServer启动，也只有它启动后，下面的才有意义。
            }

            JMXServiceURL url = new JMXServiceURL(moduleMetricsJmxUrl);
            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            connection = jmxc.getMBeanServerConnection(); // 这个connection是给其他应用程序使用的，用法见ThreadPoolMonitorProvider.monitor中的用法。
            // 在更新ModuleMetricsVisitor时，可以使用这个connection，也可以直接调用ModuleMetricsVisitor里的方法，
            // 作者在ThreadPoolMonitorProvider.monitor里用了前者，在EventNotificationListener里用了后者。作者可能是故意这样写，来学习两者的。
            // 类似的例子，在此项目中非常多，例如：
            // 1、对于多线程同步，主线程等待子线程完成后继续处理剩余的事情，作者用了CountDown，以及MoreExecutors.listeningDecorator异步的方式。
            // 2、对于进程间通信，作者用了applicationContext.publishEvent/onApplicationEvent、com.google.common.eventbus.EventBus、Observable/Observer以及Notification/NotificationListener。
            // 3、对于MXBean中使用自定义类型的属性，这些属性会自动转为CompositeDataSupport类型，但是作者还是自己写了一个CompositeDataSupport类型的属性，
            //      见ModuleMetricsVisitor.buildErrorCompositeDate方法，其实跟ModuleMetricsVisitor.setLastStackTrace途径差不多的，
            //      但是作者在EventNotificationListener里用两种方法去写了。
            // 4、等等。
            // 刻意练习。
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }
}
