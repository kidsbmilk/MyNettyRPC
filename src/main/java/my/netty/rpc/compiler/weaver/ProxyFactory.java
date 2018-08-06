package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.invoke.ObjectInvoker;

public class ProxyFactory extends AbstractProxyFactory {

    private static final ClassCache PROXY_CLASS_CACHE = new ClassCache(new ByteCodeClassTransformer()); // 注意这行代码，把ByteCodeClassTransformer关联起来了

    @Override
    <T> T createProxyImpl(ClassLoader classLoader, ObjectInvoker invoker, final Class<?>... proxyClasses) {
        Class<?> proxyClass = PROXY_CLASS_CACHE.getProxyClass(classLoader, proxyClasses);
        try {
            T result = (T) proxyClass.getConstructor(ObjectInvoker.class).newInstance(invoker);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
