package my.netty.rpc.compiler;

public interface Compiler {

    Class<?> compile(String code, ClassLoader classLoader);
}
