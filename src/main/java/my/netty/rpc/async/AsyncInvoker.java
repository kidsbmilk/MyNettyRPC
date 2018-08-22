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

    /**
     * Callback是并发编程的一种技术，即回调技术，在Java中，并不存在Callback这个类（目前没接触到），Java实现Callback这一回调技术的方式是Callable、Runnable以及Future。
     * AsyncCallback是在此项目中用于实现异步回调而定义的类。
     */
    private <T> AsyncFuture<T> submit(Callable<T> task) { // 这个函数才是最根本的。上面的重载函数最终会转到这里。
        AsyncFuture future = new AsyncFuture<T>(task); // AsyncFuture继承自FutureTask，做了一些统计工作，相当于装饰器。
        executor.submit(future); // 这个submit也是返回一个future，是非阻塞的。
        // 这里才是异步调用开始的地方。
        // 对于AsyncRpcCallTest中的调用，这里会执行calculate.calculate();然后调用MessageSendProxy.handleInvocation，之后就在MessageCallBack.start()里等待远程调用返回的结果，这也是之前版本中非async的套路。
        // 而这个future也会在AsyncCallResult.loadFuture里用来等待MessageCallBack.start()里返回的结果。
        return future;
    }

    /**
     * 反射 Reflect Modifier 修饰符工具类
     * https://www.cnblogs.com/baiqiantao/p/7478523.html
     *
     * 这里面一些if条件直接调用callback.call()，是因为要么是无法使用cglib生成动态代理或者是没必要进行动态代理。
     * 那个returnClass.isArray()以及returnClass == Object.class为什么也是直接调用callback.call呢？ ?zz? TODO-THIS
     */
    private <R> R intercept(final AsyncCallback<R> callback, Class<?> returnClass) {
        if(!Modifier.isPublic(returnClass.getModifiers())) { // 注意：这个前面有个取反的符号，表示返回类型不是公有的。
            return callback.call();
        } else if(Modifier.isFinal(returnClass.getModifiers())) { // 返回类型是final的。
            return callback.call();
        } else if(Void.TYPE.isAssignableFrom(returnClass)) { // 返回类型与void相等或者是void的子类
            return callback.call();
        } else if(returnClass.isPrimitive() || returnClass.isArray()) { // 返回类型是私有的或者是数组类型
            return callback.call();
        } else if(returnClass == Object.class) { // 返回类型是Object
            return callback.call();
        } else { // 对于AsyncRpcCallTest中的例子，返回类型为my.netty.rpc.services.pojo.CostTime，是公有的。
            return submit(callback, returnClass);
        }
    }

    private <R> R submit(final AsyncCallback<R> callback, Class<?> returnClass) {
        Future future = submit(new Callable() {
            @Override
            public R call() throws Exception {
                return callback.call();
            }
        }); // 通过这个future可以查询异步调用的结果。这里又进一步封装成了AsyncCallResult，AsyncCallResult.loadFuture代理了future.get操作。
        // 这里之所以将future封装成AsyncCallResult，是为了创建二次代理。
        AsyncCallResult result = new AsyncCallResult(returnClass, future, RpcSystemConfig.SYSTEM_PROPERTY_ASYNC_MESSAGE_CALLBACK_TIMEOUT);
        R asyncProxy = (R) result.getResult();

        return asyncProxy;
    }
}
