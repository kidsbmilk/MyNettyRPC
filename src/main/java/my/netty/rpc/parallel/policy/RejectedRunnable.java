package my.netty.rpc.parallel.policy;

public interface RejectedRunnable extends Runnable {

    void rejected();
}
