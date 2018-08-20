package my.netty.rpc.jmx.system;

import my.netty.rpc.netty.MessageRecvExecutor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.*;
import org.springframework.jmx.support.ConnectorServerFactoryBean;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.remoting.rmi.RmiRegistryFactoryBean;

import javax.management.*;
import java.io.IOException;

/**
 * 一些文档：
 * spring4 集成JMX监控：https://blog.csdn.net/love13135816/article/details/71172050
 * JMX (三)--------spring整合JMX：http://90haofang-163-com.iteye.com/blog/1904451
 * Spring JMX之一：使用JMX管理Spring Bean：https://www.cnblogs.com/duanxz/p/3968308.html
 * JMX之将Spring Bean 输出为JMX并为远程服务暴露Mbean：http://wujiu.iteye.com/blog/2179210
 * spring与jmx：http://macrochen.iteye.com/blog/246178
 * Spring 框架参考文档(六)-Integration之JMX：https://blog.csdn.net/xiangjai/article/details/53991505
 *
 *
 */

@Configuration
@EnableMBeanExport // 这个相当于添加MBeanServer
@ComponentScan("my.netty.rpc.jmx")
public class ThreadPoolMonitorProvider {

    public static final String DELIMITER = ":";
    public static final String JMX_POOL_SIZE_METHOD = "setPoolSize";
    public static final String JMX_ACTIVE_COUNT_METHOD = "setActiveCount";
    public static final String JMX_CORE_POOL_SIZE_METHOD = "setCorePoolSize";
    public static final String JMX_MAXIMUM_POOL_SIZE_METHOD = "setMaximumPoolSize";
    public static final String JMX_LARGEST_POOL_SIZE_METHOD = "setLargestPoolSize";
    public static final String JMX_TASK_COUNT_METHOD = "setTaskCount";
    public static final String JMX_COMPLETED_TASK_COUNT_METHOD = "setCompletedTaskCount";
    public static String url;

    @Bean
    public ThreadPoolStatus threadPoolStatus() {
        return new ThreadPoolStatus(); // ThreadPoolStatus已有ManagedResource注解。
    }

    @Bean
    public MBeanServerFactoryBean mbeanServer() { // MBeanServerFactoryBean用于得到一个本地的MBeanServer。
        MBeanServerFactoryBean mBeanServerFactoryBean = new MBeanServerFactoryBean();
        mBeanServerFactoryBean.setLocateExistingServerIfPossible(true); // 见org.springframework.jmx.support.MBeanServerFactoryBean类里的注释。
        return mBeanServerFactoryBean;
    }

    @Bean
    public RmiRegistryFactoryBean registry() { // 这个用于得到一个本地RMI记录
        return new RmiRegistryFactoryBean();
    }

    @Bean
    @DependsOn("registry") // 注意这里，是先要处理上面的register()，才能处理下面的connectorServer()。见spring中类注释。
    public ConnectorServerFactoryBean connectorServer() throws MalformedObjectNameException { // 用于得到一个供远程客户端连接MBeanServer的连接器。
        // monitor方法通过连接这个连接器，来将状态发布到本地的MBeanServer上，而外部的jconsole可以通过这个连接器来获取状态信息，
        // 也可以通过这个连接器来更改状态信息（跟monitor里的做法类似）。
        MessageRecvExecutor ref = MessageRecvExecutor.getInstance();
        String ipAddr = StringUtils.isNotEmpty(ref.getServerAddress()) ? StringUtils.substringBeforeLast(ref.getServerAddress(), DELIMITER) : "localhost";
        url = "service:jmx:rmi://" + ipAddr + "/jndi/rmi://" + ipAddr + ":1099/nettyrpcstatus";
        System.out.println("NettyRPC JMX MonitorURL : [ " + url + " ]");
        ConnectorServerFactoryBean connectorServerFactoryBean = new ConnectorServerFactoryBean();
        connectorServerFactoryBean.setObjectName("connector:name=rmi");
        connectorServerFactoryBean.setServiceUrl(url);
        return connectorServerFactoryBean;
    }

    // 此方法在RpcThreadPool.getExecutorWithJmx里周期性的被调用来向本地的MBeanServer报告系统的状态。
    // 用来创建一个访问MBeanServer的客户端连接器, 比如MBeanServer bean暴露了一个服务器端连接器, 那么客户端就可以通过这个连接器来访问MBeanServer中的MBean.
    // 可以理解为ConnectorServerFactoryBean的对应物, server与client之间就是这两种连接器建立通讯连接
    public static void monitor(ThreadPoolStatus status) throws IOException, MalformedObjectNameException, ReflectionException, MBeanException, InstanceNotFoundException {
        MBeanServerConnectionFactoryBean mBeanServerConnectionFactoryBean = new MBeanServerConnectionFactoryBean();
        mBeanServerConnectionFactoryBean.setServiceUrl(url);
        mBeanServerConnectionFactoryBean.afterPropertiesSet();
        MBeanServerConnection connection = mBeanServerConnectionFactoryBean.getObject();
        ObjectName objectName = new ObjectName("my.netty.rpc.jmx.system:name=threadPoolStatus,type=ThreadPoolStatus");

        connection.invoke(objectName, JMX_POOL_SIZE_METHOD, new Object[]{status.getPoolSize()}, new String[]{int.class.getName()});
        connection.invoke(objectName, JMX_ACTIVE_COUNT_METHOD, new Object[]{status.getActiveCount()}, new String[]{int.class.getName()});
        connection.invoke(objectName, JMX_CORE_POOL_SIZE_METHOD, new Object[]{status.getCorePoolSize()}, new String[]{int.class.getName()});
        connection.invoke(objectName, JMX_MAXIMUM_POOL_SIZE_METHOD, new Object[]{status.getMaximumPoolSize()}, new String[]{int.class.getName()});
        connection.invoke(objectName, JMX_LARGEST_POOL_SIZE_METHOD, new Object[]{status.getLargestPoolSize()}, new String[]{int.class.getName()});
        connection.invoke(objectName, JMX_TASK_COUNT_METHOD, new Object[]{status.getTaskCount()}, new String[]{long.class.getName()});
        connection.invoke(objectName, JMX_COMPLETED_TASK_COUNT_METHOD, new Object[]{status.getCompletedTaskCount()}, new String[]{long.class.getName()});
    }
}