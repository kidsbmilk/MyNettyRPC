package my.netty.rpc.async;

import net.sf.cglib.proxy.LazyLoader;

public class AsyncCallResultInterceptor implements LazyLoader {

    private AsyncCallResult result;

    public AsyncCallResultInterceptor(AsyncCallResult result) {
        this.result = result;
    }

    public Object loadObject() throws Exception {
        return result.loadFuture();
    }
}
