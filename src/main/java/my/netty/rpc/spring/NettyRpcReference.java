package my.netty.rpc.spring;

import com.google.common.eventbus.EventBus;
import my.netty.rpc.event.ClientStopEvent;
import my.netty.rpc.event.ClientStopEventListener;
import my.netty.rpc.netty.MessageSendExecutor;
import my.netty.rpc.serialize.RpcSerializeProtocol;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class NettyRpcReference implements FactoryBean, InitializingBean, DisposableBean {

    private String interfaceName;
    private String ipAddr;
    private String protocol;
    private EventBus eventBus = new EventBus();

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void destroy() throws Exception {
        // 反射创建对象，然后就可以不同的监听器方法输出不同的信息了。
        eventBus.post(new ClientStopEvent(interfaceName));
    }

    /**
     * 最先执行的是postProcessBeforeInitialization，然后是afterPropertiesSet，然后是init-method，然后是postProcessAfterInitialization。
     * https://blog.csdn.net/u013013553/article/details/79038702
     */
    public void afterPropertiesSet() throws Exception {
        MessageSendExecutor.getInstance().setRpcServerLoader(ipAddr, RpcSerializeProtocol.valueOf(protocol));
        ClientStopEventListener listener = new ClientStopEventListener();
        // Google EventBus 使用详解
        // https://blog.csdn.net/zhglance/article/details/54314823
        eventBus.register(listener);
    }

    public Object getObject() throws Exception {
        return MessageSendExecutor.getInstance().execute(getObjectType());
    }

    public Class<?> getObjectType() {
        try {
            return this.getClass().getClassLoader().loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            System.err.println("spring analyze fail!");
        }
        return null;
    }

    public boolean isSingleton() {
        return true;
    }
}
