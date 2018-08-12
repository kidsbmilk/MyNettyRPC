package my.netty.rpc.jmx;

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

public class ModuleMetricsHandler extends AbstractModuleMetricsHandler {

    private static final ModuleMetricsHandler INSTANCE = new ModuleMetricsHandler();
    private MBeanServerConnection connection;

    public static ModuleMetricsHandler getInstance() {
        return INSTANCE;
    }

    private ModuleMetricsHandler() {
    }

    protected ModuleMetricsVisitor visitCriticalSection(String moduleName, String methodName) {
        final String method = methodName.trim();
        final String module = moduleName.trim();

        // FIXME: JMX度量临界区要注意线程间的并发竞争，否则会统计数据失真
        Iterator iterator = new FilterIterator(visitorList.iterator(), new Predicate() {
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

    public void start() {
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

                    System.out.printf("NettyRPC JMX server is start success!\nurl:[ %s ]\n\n", moduleMetricsJmxUrl);
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
            if(!semaphoreWrapper.isRelease()) {
                semaphoreWrapper.acquire();
            }

            JMXServiceURL url = new JMXServiceURL(moduleMetricsJmxUrl);
            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            connection = jmxc.getMBeanServerConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return connection;
        }
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }
}
