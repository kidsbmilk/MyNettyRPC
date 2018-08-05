package my.netty.rpc.compiler.weaver;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

public class ClassCache {

    // FIXME:
    // 引入弱引用节省内存消耗，JVM每次gc的时候，自动释放内存
    private final Map<ClassLoader, Map<Set<Class<?>>, WeakReference<Class<?>>>> loader = new HashMap<>();

    private final Transformer transformer;

    public ClassCache(Transformer transformer) {
        this.transformer = transformer;
    }

    private Map<Set<Class<?>>, WeakReference<Class<?>>> getClassCache(ClassLoader classLoader) {
        Map<Set<Class<?>>, WeakReference<Class<?>>> cache = loader.get(classLoader);
        if(cache == null) {
            cache = new HashMap<>();
            loader.put(classLoader, cache);
        }

        return cache;
    }

    private Set<Class<?>> toClassCacheKey(Class<?>[] proxyClasses) {
        return new HashSet<>(Arrays.asList(proxyClasses));
    }

    public synchronized Class<?> getProxyClass(ClassLoader classLoader, Class<?>[] proxyClasses) {
        Map<Set<Class<?>>, WeakReference<Class<?>>> classCache = getClassCache(classLoader);
        Set<Class<?>> key = toClassCacheKey(proxyClasses);
        Class<?> proxyClass;
        Reference<Class<?>> proxyClassReference = classCache.get(key);

        if(proxyClassReference == null) {
            proxyClass = transformer.transform(classLoader, proxyClasses);
            classCache.put(key, new WeakReference<>(proxyClass));
        } else {
            proxyClass = proxyClassReference.get();
            if(proxyClass == null) {
                proxyClass = transformer.transform(classLoader, proxyClasses);
                classCache.put(key, new WeakReference<>(proxyClass));
            }
        }

        return proxyClass;
    }
}
