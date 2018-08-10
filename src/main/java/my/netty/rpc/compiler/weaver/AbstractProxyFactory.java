package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.intercept.Interceptor;
import my.netty.rpc.compiler.invoke.InterceptorInvoker;
import my.netty.rpc.compiler.invoke.ObjectInvoker;

public abstract class AbstractProxyFactory implements ClassProxyFactory {

    @Override
    public <T> T createProxy(Object target, Interceptor interceptor, Class<?>... proxyClasses) {
        return createProxy(Thread.currentThread().getContextClassLoader(), target, interceptor, proxyClasses);
        // 这个获取的线程上下文加载器与AccessAdaptiveProvider.invoke里加载编译后的类的加载器是一样的。
        // 所以，现在编译后的用户类是可见的。
    }

    @Override
    public <T> T createProxy(ClassLoader classLoader, Object target, Interceptor interceptor, Class<?>... proxyClasses) { // 这个是接口ClassProxy里的另一个方法
        return createProxyImpl(classLoader, new InterceptorInvoker(target, interceptor), proxyClasses);
        // 这个ObjectInvoker是将target与interceptor关联起来，也是拦截动作的起点，见InterceptorInvoker.invokeImpl的实现。
    }

    abstract <T> T createProxyImpl(ClassLoader classLoader, ObjectInvoker interceptorInvoker, Class<?>... proxyClasses); // 创建自己的抽象方法，使类继承以及接口实现层次更清晰。
}
