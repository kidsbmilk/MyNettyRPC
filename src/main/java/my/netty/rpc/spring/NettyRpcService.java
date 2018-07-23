package my.netty.rpc.spring;

import my.netty.rpc.event.ServerStartEvent;
import my.netty.rpc.netty.MessageRecvExecutor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class NettyRpcService implements ApplicationContextAware, ApplicationListener {

    private String interfaceName;

    private String ref;

    private ApplicationContext applicationContext;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        MessageRecvExecutor.getInstance().getHandlerMap().put(interfaceName, applicationContext.getBean(ref));
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        applicationContext.publishEvent(new ServerStartEvent(new Object()));
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
