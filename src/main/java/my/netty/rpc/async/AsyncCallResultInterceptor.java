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

    public Object loadObject() throws Exception {
        return result.loadFuture();
    }
}
