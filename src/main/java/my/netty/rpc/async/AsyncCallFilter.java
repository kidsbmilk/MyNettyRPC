package my.netty.rpc.async;

import net.sf.cglib.proxy.CallbackFilter;

import java.lang.reflect.Method;

public class AsyncCallFilter implements CallbackFilter {

    public int accept(Method method) {
        return AsyncCallObject.class.isAssignableFrom(method.getDeclaringClass()) ? 1 : 0;
    }
}
