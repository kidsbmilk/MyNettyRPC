package my.netty.rpc.test;

import my.netty.rpc.async.AsyncCallObject;
import my.netty.rpc.async.AsyncCallback;
import my.netty.rpc.async.AsyncInvoker;
import my.netty.rpc.services.CostTimeCalculate;
import my.netty.rpc.services.pojo.CostTime;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 总结async的实现：
 * 这里的async的实现是通过返回代理对象以及拦截、延迟加载来实现的。本质上还是使用方去主动得到对象，如果对象不使用，则可能一直不会触发真实的对象（这是延迟加载的作用）。
 * 这里虽然是主动得到对象，但是线程在创建一个对象后，并没有说非得得到对象的结果后才能继续运行，而是创建完一个对象后又继承做别的去了，所以也是异步非阻塞的。
 *
 * 这种实现方式，相比于Callback（回调）是不一样的：回调是在结果得到后主动去执行已经设置好的回调函数，比如RpcServerLoader.load中的ListenableFuture的使用。
 *
 * 这里的async的实现相比于Callback的优势在于，可以决定对象何时真正被使用，而不像Callback一样，只要被设定了且在执行前没被取消，就会在得到对象后立即执行回调函数。
 */
public class AsyncRpcCallTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        final CostTimeCalculate calculate = (CostTimeCalculate) context.getBean("costTime"); // 这个calculate是MessageSendProxy类型的。

        long start = 0, end = 0;
        start = System.currentTimeMillis();

        AsyncInvoker invoker = new AsyncInvoker();

        CostTime elapse0 = invoker.submit(new AsyncCallback<CostTime>() {
            public CostTime call() {
                return calculate.calculate(); // 异步调用的关键是把这个实质操作给异步执行了。
            }
        });

        CostTime elapse1 = invoker.submit(new AsyncCallback<CostTime>() {
            public CostTime call() {
                return calculate.calculate();
            }
        });

        CostTime elapse2 = invoker.submit(new AsyncCallback<CostTime>() {
            public CostTime call() {
                return calculate.calculate();
            }
        });

        /**
         * 关于java中任意对象强制转换为接口类型的问题
         * https://www.cnblogs.com/aheiabai/p/5850998.html
         *
         * 在AsyncCallResult.getResult()中设置了要拦截AsyncCallObject的实现类的_getStatus方法，具体拦截后的动作见AsyncCallObjectInterceptor中的实现。
         * 所以，在这里对对象进行强制类型转换后的方法调用在编译时（编译器不对接口强制类型转换做检查）和运行时（使用了方法拦截）都不会出错，而且达到了目的。
         */

        // CGLIB(Code Generation Library)详解
        // https://blog.csdn.net/danchu/article/details/70238002
        // AsyncRpcCallTest中的以下语句中：
        // System.out.println("1 async nettyrpc call:[" + "result:" + elapse0 + ", status:[" + ((AsyncCallObject) elapse0)._getStatus() + "]");
        // 会调用elapse0.toString()，然后会被AsyncCallResultInterceptor拦截，调用loadObject()方法。
        // 这是延迟加载，当AsyncRpcCallTest中开始使用这个对象时，才会拦截加载。
        System.out.println("1 async nettyrpc call:[" + "result:" + elapse0 + ", status:[" + ((AsyncCallObject) elapse0)._getStatus() + "]");
        System.out.println("2 async nettyrpc call:[" + "result:" + elapse1 + ", status:[" + ((AsyncCallObject) elapse1)._getStatus() + "]");
        System.out.println("3 async nettyrpc call:[" + "result:" + elapse2 + ", status:[" + ((AsyncCallObject) elapse2)._getStatus() + "]");

        end = System.currentTimeMillis();

        System.out.println("nettyrpc async calculate time:" + (end - start));

        context.destroy();
    }
}
