package my.netty.rpc.async;

public interface AsyncCallback<R> {

    R call();
}
