package my.netty.rpc.async;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.exception.AsyncCallException;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncCallResult {

    private Class returnClass;
    private Future future;
    private Long timeout;

    public AsyncCallResult(Class returnClass, Future future, Long timeout) {
        this.returnClass = returnClass;
        this.future = future;
        this.timeout = timeout;
    }

    public Object loadFuture() throws AsyncCallException {
        try {
            if(timeout <= 0L) {
                return future.get();
            } else {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AsyncCallException(e);
        } catch (InterruptedException e) {
            throw new AsyncCallException(e);
        } catch (Exception e) {
            throw new AsyncCallException(e);
        }
    }

    public Object getResult() {
        Class proxyClass = AsyncProxyCache.get(returnClass.getName());
        if(proxyClass == null) {
            /**
             * 一开始我引入的包是cglib，在这里会出错，然后引入cglib-nodep就可以了。
             * 原因：cglib-nodep里包含了asm包，cglib里不包含asm包。asm包和cglib不匹配也会出错。因此用cglib-nodep就不会出现版本不匹配情况
             * https://zhidao.baidu.com/question/172397861.html
             */
            /**
             * cglib系列之二 CallbackFilter
             * http://blog.163.com/chen_chenluoxi/blog/static/202159015201631311475636/
             */
            Enhancer enhancer = new Enhancer();
            if(returnClass.isInterface()) {
                enhancer.setInterfaces(new Class[]{AsyncCallObject.class, returnClass}); // 设置要拦截的类需要实现的接口
            } else {
                enhancer.setInterfaces(new Class[]{AsyncCallObject.class}); // 设置要拦截的类需要实现的接口
                enhancer.setSuperclass(returnClass); // 设置要拦截的类需要继承的父类，感觉这个returnClass这个变量名起的不好。
            }
            enhancer.setCallbackFilter(new AsyncCallFilter());
            enhancer.setCallbackTypes(new Class[]{AsyncCallResultInterceptor.class, AsyncCallObjectInterceptor.class});
            proxyClass = enhancer.createClass();
            AsyncProxyCache.save(returnClass.getName(), proxyClass);
        }

        Enhancer.registerCallbacks(proxyClass, new Callback[]{new AsyncCallResultInterceptor(this),
                new AsyncCallObjectInterceptor(future)});

        try {
            return ReflectionUtils.newInstance(proxyClass);
        } finally {
            Enhancer.registerStaticCallbacks(proxyClass, null);
        }
    }
}
