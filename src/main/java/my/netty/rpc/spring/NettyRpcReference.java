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

    // 这是DisposableBean里的方法，在Bean销毁时调用。
    // InitializingBean和DisposableBean
    // https://blog.csdn.net/msoso_______1988/article/details/9371467
    public void destroy() throws Exception {
        // 反射创建对象，然后就可以不同的监听器方法输出不同的信息了。
        eventBus.post(new ClientStopEvent(0));
    }

    /**
     * 最先执行的是postProcessBeforeInitialization，然后是afterPropertiesSet，然后是init-method，然后是postProcessAfterInitialization。
     * https://blog.csdn.net/u013013553/article/details/79038702
     */
    public void afterPropertiesSet() throws Exception {
        MessageSendExecutor.getInstance().setRpcServerLoader(ipAddr, RpcSerializeProtocol.valueOf(protocol)); // 这里执行多少次，就会有多少个连接被建立。
        // 而rpc-invoke-config-client中nettyrpc:reference的个数决定了这里被调用多少次。
        ClientStopEventListener listener = new ClientStopEventListener();
        // Google EventBus 使用详解
        // https://blog.csdn.net/zhglance/article/details/54314823
        eventBus.register(listener);
    }

    // 在ClassPathXmlApplicationContext.getBean中取出对象时，会调用这个方法，然后对对象进行代理，进而调用MessageSendProxy.handleInvocation，
    // 将对象发送到RPC服务器上去执行相应操作。见PojoCallTest中的实现。
    // spring中FactoryBean中的getObject()方法的作用
    // https://blog.csdn.net/liuxiao723846/article/details/73794128
    public Object getObject() throws Exception {
        return MessageSendExecutor.getInstance().execute(getObjectType()); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。
    }

    public Class<?> getObjectType() {
        try {
            return this.getClass().getClassLoader().loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            System.err.println("spring analyze fail!");
        }
        return null;
    }

    // 如果isSingleton()返回true,则该实例会放到Spring容器的单实例缓存池中。
    public boolean isSingleton() {
        return true;
    }
}
