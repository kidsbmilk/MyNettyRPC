package my.netty.rpc.spring;

import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.jmx.ThreadPoolMonitorProvider;
import my.netty.rpc.netty.MessageRecvExecutor;
import my.netty.rpc.serialize.RpcSerializeProtocol;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NettyRpcRegistry implements InitializingBean, DisposableBean {

    private String ipAddr;
    private String protocol;
    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    public void destroy() throws Exception {
        MessageRecvExecutor.getInstance().stop();
    }

    public void afterPropertiesSet() throws Exception {
        MessageRecvExecutor ref = MessageRecvExecutor.getInstance();
        ref.setServerAddress(ipAddr);
        ref.setSerializeProtocol(Enum.valueOf(RpcSerializeProtocol.class, protocol));

        if(RpcSystemConfig.isMonitorServerSupport()) {
            context.register(ThreadPoolMonitorProvider.class);
            context.refresh();
        }

        ref.start();
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
}
