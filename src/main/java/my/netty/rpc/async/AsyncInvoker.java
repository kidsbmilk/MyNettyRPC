package my.netty.rpc.async;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.exception.AsyncCallException;
import my.netty.rpc.parallel.RpcThreadPool;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class AsyncInvoker {

    private ThreadPoolExecutor executor = (ThreadPoolExecutor) RpcThreadPool.getExecutor(RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_THREAD_NUMS, RpcSystemConfig.SYSTEM_PROPERTY_THREADPOOL_QUEUE_NUMS);

    /**
     * 类方法学习四:getGenericInterfaces,getInterfaces
     * http://zzc1684.iteye.com/blog/2147133
     */
    /**
     * java中<T> T和T的区别？
     * https://segmentfault.com/q/1010000009171736
     */
    public <R> R submit(final AsyncCallback<R> callback) {
        Type type = callback.getClass().getGenericInterfaces()[0]; //　这里只所以只取第一个，见AsyncRpcCallTest中的例子，是直接创建了匿名类。
        if(type instanceof ParameterizedType) {
            Class returnClass = (Class) ReflectionUtils.getGenericClass((ParameterizedType) type, 0);
            return intercept(callback, returnClass);
        } else {
            throw new AsyncCallException("NettyRPC AsyncCallback must be parameterized type!");
        }
    }

    private <T> AsyncFuture<T> submit(Callable<T> task) {
        AsyncFuture future = new AsyncFuture<T>(task);
        executor.submit(future);
        return future;
    }

    /**
     * 反射 Reflect Modifier 修饰符工具类
     * https://www.cnblogs.com/baiqiantao/p/7478523.html
     */
    private <R> R intercept(final AsyncCallback<R> callback, Class<?> returnClass) {
        if(!Modifier.isPublic(returnClass.getModifiers())) {
            return callback.call();
        } else if(Modifier.isFinal(returnClass.getModifiers())) {
            return callback.call();
        } else if(Void.TYPE.isAssignableFrom(returnClass)) {
            return callback.call();
        } else if(returnClass.isPrimitive() || returnClass.isArray()) {
            return callback.call();
        } else if(returnClass == Object.class) {
            return callback.call();
        } else {
            return submit(callback, returnClass);
        }
    }

    private <R> R submit(final AsyncCallback<R> callback, Class<?> returnClass) {
        Future future = submit(new Callable() {
            public R call() throws Exception {
                return callback.call();
            }
        });

        AsyncCallResult result = new AsyncCallResult(returnClass, future, RpcSystemConfig.SYSTEM_PROPERTY_ASYNC_MESSAGE_CALLBACK_TIMEOUT);
        R asyncProxy = (R) result.getResult();

        return asyncProxy;
    }
}
