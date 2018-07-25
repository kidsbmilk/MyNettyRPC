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
        // 这里保存了所有可用的服务
        MessageRecvExecutor.getInstance().getHandlerMap().put(interfaceName, applicationContext.getBean(ref));
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * 这个方法在postProcessBeforeInitialization()中调用。
     * https://blog.csdn.net/xtj332/article/details/20127501
     * 最先执行的是postProcessBeforeInitialization，然后是afterPropertiesSet，然后是init-method，然后是postProcessAfterInitialization。
     * https://blog.csdn.net/u013013553/article/details/79038702
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 当NettyRpcServiceParser解析完一个服务后，会发通知，然后onApplicationEvent方法将此服务加入到handlerMap中。
        // 所有通知是会缓存的，内部实现可能是一个队列，可以调试看看。
        applicationContext.publishEvent(new ServerStartEvent(new Object()));
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
