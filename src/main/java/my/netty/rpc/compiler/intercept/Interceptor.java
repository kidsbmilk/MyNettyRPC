package my.netty.rpc.compiler.intercept;

public interface Interceptor {

    Object intercept(Invocation invocation) throws Throwable;
}
