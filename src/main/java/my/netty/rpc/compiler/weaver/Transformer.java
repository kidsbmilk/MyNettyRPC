package my.netty.rpc.compiler.weaver;

public interface Transformer {

    Class<?> transform(ClassLoader classLoader, Class<?>... proxyClasses);
}
