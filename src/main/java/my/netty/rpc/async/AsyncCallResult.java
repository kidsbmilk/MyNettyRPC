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
             *
             * cglib系列之一 Enhancer和MethodInterceptor
             * http://blog.163.com/chen_chenluoxi/blog/static/2021590152016313104530795/
             *
             * 这里是又创建了一个代理对象，在代理对象之上，设置拦截器，以达到不同的效果。
             * async部分，是在非async部分的基础上实现的，在非async部分上，已经创建过一个代理对象了（见NettyRpcReference.getObject的实现），而这里在其基础上又通过Enhance创建了代理对象，算是二次代理了。
             */
            Enhancer enhancer = new Enhancer();
            if(returnClass.isInterface()) {
                enhancer.setInterfaces(new Class[]{AsyncCallObject.class, returnClass});
            } else {
                enhancer.setInterfaces(new Class[]{AsyncCallObject.class});
                enhancer.setSuperclass(returnClass);
            }
            enhancer.setCallbackFilter(new AsyncCallFilter());
            enhancer.setCallbackTypes(new Class[]{AsyncCallResultInterceptor.class, AsyncCallObjectInterceptor.class});
            proxyClass = enhancer.createClass();
            AsyncProxyCache.save(returnClass.getName(), proxyClass); // 缓存代理对象
        }

        Enhancer.registerCallbacks(proxyClass, new Callback[]{new AsyncCallResultInterceptor(this),
                new AsyncCallObjectInterceptor(future)}); // 对于async部分的代码，关键部分就在这里，多个同类对象共用一个缓存的代理对象，但是注册的拦截不同，所以调用的具体对象不同。见方法说明。

        try {
            return ReflectionUtils.newInstance(proxyClass); // 创建一个Enhance代理对象。
        } finally {
            Enhancer.registerStaticCallbacks(proxyClass, null); // 见方法说明。
        }
    }
}
