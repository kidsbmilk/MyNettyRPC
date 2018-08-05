package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.intercept.Interceptor;
import my.netty.rpc.compiler.invoke.InterceptorInvoker;
import my.netty.rpc.compiler.invoke.ObjectInvoker;

public class ProxyProvider extends AbstractProxyProvider {

    private static final ClassCache PROXY_CLASS_CACHE = new ClassCache(new ByteCodeClassTransformer());

    @Override
    public <T> T createProxy(ClassLoader classLoader, Object target, Interceptor interceptor, Class<?>... proxyClasses) {
        return createProxy(classLoader, new InterceptorInvoker(target, interceptor), proxyClasses);
    }

    private <T> T createProxy(ClassLoader classLoader, ObjectInvoker invoker, final Class<?>... proxyClasses) {
        Class<?> proxyClass = PROXY_CLASS_CACHE.getProxyClass(classLoader, proxyClasses);
        try {
            T result = (T) proxyClass.getConstructor(ObjectInvoker.class).newInstance(invoker);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
