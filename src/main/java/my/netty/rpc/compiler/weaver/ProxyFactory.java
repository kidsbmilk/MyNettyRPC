package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.invoke.ObjectInvoker;

public class ProxyFactory extends AbstractProxyFactory {

    private static final ClassCache PROXY_CLASS_CACHE = new ClassCache(new ByteCodeAbstractClassTransformer()); // 注意这行代码，把ByteCodeClassTransformer关联起来了

    @Override
    <T> T createProxyImpl(ClassLoader classLoader, ObjectInvoker invoker, final Class<?>... proxyClasses) {
        Class<?> proxyClass = PROXY_CLASS_CACHE.getProxyClass(classLoader, proxyClasses); // 这里会调用ByteCodeClassTransformer里的方法生成并加载类
        try {
            T result = (T) proxyClass.getConstructor(ObjectInvoker.class).newInstance(invoker);
            // 这个invoker就是AccessAdapterProvider.invoke中的代码传入的new SimpleMethodInterceptor()，然后用这个invoker去初始化上面创建的proxyClass，
            // 见ByteCodeClassTransformer的说明，可以知道proxyClass有带有一个参数的构造函数，这里生成一个对象并返回。
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
