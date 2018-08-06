package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.intercept.Interceptor;
import my.netty.rpc.compiler.invoke.InterceptorInvoker;
import my.netty.rpc.compiler.invoke.ObjectInvoker;

public abstract class AbstractProxyFactory implements ClassProxyFactory {

    @Override
    public <T> T createProxy(Object target, Interceptor interceptor, Class<?>... proxyClasses) {
        return createProxy(Thread.currentThread().getContextClassLoader(), target, interceptor, proxyClasses);
    }

    @Override
    public <T> T createProxy(ClassLoader classLoader, Object target, Interceptor interceptor, Class<?>... proxyClasses) { // 这个是接口ClassProxy里的另一个方法
        return createProxyImpl(classLoader, new InterceptorInvoker(target, interceptor), proxyClasses);
    }

    abstract <T> T createProxyImpl(ClassLoader classLoader, ObjectInvoker interceptorInvoker, Class<?>... proxyClasses); // 创建自己的抽象方法，使类继承以及接口实现层次更清晰。
}
