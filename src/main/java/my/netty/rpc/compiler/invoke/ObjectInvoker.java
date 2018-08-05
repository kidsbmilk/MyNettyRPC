package my.netty.rpc.compiler.invoke;

import java.lang.reflect.Method;

public interface ObjectInvoker {

    Object invoke(Object proxy, Method method, Object... args) throws Throwable;
}
