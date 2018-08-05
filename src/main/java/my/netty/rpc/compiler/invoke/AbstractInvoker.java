package my.netty.rpc.compiler.invoke;

import my.netty.rpc.core.ReflectionUtils;

import java.lang.reflect.Method;

public abstract class AbstractInvoker implements ObjectInvoker {

    @Override
    public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
        // FIXME: Object类的方法没有必要字节码增强，这里直接判断返回
        if(ReflectionUtils.isHashCodeMethod(method)) {
            return Integer.valueOf(System.identityHashCode(proxy));
        }

        if(ReflectionUtils.isEqualsMethod(method)) {
            return Boolean.valueOf(proxy == args[0]);
        }

        return invokeImpl(proxy, method, args);
    }

    public abstract Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable;
}
