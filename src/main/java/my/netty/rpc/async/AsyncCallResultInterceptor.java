package my.netty.rpc.async;

import net.sf.cglib.proxy.LazyLoader;

/**
 * 实战CGLib系列之proxy篇(三)：延迟加载LazyLoader
 * http://shensy.iteye.com/blog/1881277
 */
public class AsyncCallResultInterceptor implements LazyLoader {

    private AsyncCallResult result;

    public AsyncCallResultInterceptor(AsyncCallResult result) {
        this.result = result;
    }

    // CGLIB(Code Generation Library)详解
    // https://blog.csdn.net/danchu/article/details/70238002
    // AsyncRpcCallTest中的以下语句中：
    // System.out.println("1 async nettyrpc call:[" + "result:" + elapse0 + ", status:[" + ((AsyncCallObject) elapse0)._getStatus() + "]");
    // 会调用elapse0.toString()，然后会被AsyncCallResultInterceptor拦截，调用loadObject()方法。
    // 这是延迟加载，当AsyncRpcCallTest中开始使用这个对象时，才会拦截加载。
    @Override
    public Object loadObject() throws Exception {
//        System.out.println(System.currentTimeMillis());
//        System.out.println("test");
        return result.loadFuture();
    }
}
