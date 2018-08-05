package my.netty.rpc.compiler.invoke;

import my.netty.rpc.compiler.intercept.Interceptor;
import my.netty.rpc.compiler.intercept.InvocationProvider;

import java.lang.reflect.Method;

public class InterceptorInvoker extends AbstractInvoker {

    private final Object target;

    private final Interceptor methodInterceptor;

    public InterceptorInvoker(Object target, Interceptor methodInterceptor) {
        this.target = target;
        this.methodInterceptor = methodInterceptor;
    }

    @Override
    public Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable {
        InvocationProvider invocation = new InvocationProvider(target, proxy, method, args);
        return methodInterceptor.intercept(invocation);
    }
}
