package my.netty.rpc.compiler;

public interface AccessAdaptive {

    Object invoke(String code, String method, Object[] args);
}
