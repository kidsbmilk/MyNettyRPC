package my.netty.rpc.spring;

import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.jmx.ModuleMetricsHandler;
import my.netty.rpc.jmx.ThreadPoolMonitorProvider;
import my.netty.rpc.netty.MessageRecvExecutor;
import my.netty.rpc.serialize.RpcSerializeProtocol;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NettyRpcRegistry implements InitializingBean, DisposableBean {

    private String ipAddr;
    private String protocol;
    private String echoApiPort;
    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    public void destroy() throws Exception {
        MessageRecvExecutor.getInstance().stop();

        if(RpcSystemConfig.SYSTEM_PROPERTY_JMX_INVOKE_METRICS != 0) {
            ModuleMetricsHandler.getInstance().stop();
        }
    }

    /**
     * 最先执行的是postProcessBeforeInitialization，然后是afterPropertiesSet，然后是init-method，然后是postProcessAfterInitialization。
     * https://blog.csdn.net/u013013553/article/details/79038702
     */
    public void afterPropertiesSet() throws Exception {
        MessageRecvExecutor ref = MessageRecvExecutor.getInstance();
        ref.setServerAddress(ipAddr);
        ref.setEchoApiPort(Integer.parseInt(echoApiPort));
        ref.setSerializeProtocol(Enum.valueOf(RpcSerializeProtocol.class, protocol));

        if(RpcSystemConfig.isMonitorServerSupport()) {
            context.register(ThreadPoolMonitorProvider.class);
            context.refresh();
        }

        ref.start();

        if(RpcSystemConfig.SYSTEM_PROPERTY_JMX_INVOKE_METRICS != 0) {
            ModuleMetricsHandler.getInstance().start();
        }
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getEchoApiPort() {
        return echoApiPort;
    }

    public void setEchoApiPort(String echoApiPort) {
        this.echoApiPort = echoApiPort;
    }
}
