package my.netty.rpc.compiler.invoke;

import my.netty.rpc.compiler.intercept.Interceptor;
import my.netty.rpc.compiler.intercept.InvocationProvider;

import java.lang.reflect.Method;

public class InterceptorInvoker extends AbstractInvoker {

    private final Object target;

    private final Interceptor methodInterceptor;

    public InterceptorInvoker(Object target, Interceptor methodInterceptor) { // 注意这里，证明了这个类的目的和作用就是沟通ObjectInvoker与Interceptor的。
        this.target = target;
        this.methodInterceptor = methodInterceptor;
    }

    // 注意：这里还继承了父类AbstractInvoker的方法：public Object invoke(Object proxy, Method method, Object... args) throws Throwable

    @Override
    public Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable {
        InvocationProvider invocation = new InvocationProvider(target, proxy, method, args);
        return methodInterceptor.intercept(invocation); // interceptor要拦截的对象是Invocation类型的，然后这里使用InvocationProvider来包装Invoker里的参数。
    }
}
