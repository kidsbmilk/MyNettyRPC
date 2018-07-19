package my.netty.rpc.servicebean;

import my.netty.rpc.core.MessageSendExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalcParallelRequestThread implements Runnable {

    private CountDownLatch signal;
    private CountDownLatch finish;
    private MessageSendExecutor executor;
    private int taskNumber = 0;

    public CalcParallelRequestThread(MessageSendExecutor executor, CountDownLatch signal, CountDownLatch finish, int taskNumber) {
        this.signal = signal;
        this.finish = finish;
        this.taskNumber = taskNumber;
        this.executor = executor;
    }

    public void run() {
        try {
            signal.await();

            Calculate calc = executor.execute(Calculate.class);
            /**
             * 这里通过guava反射来创建动态代理对象，下面的calc.add操作，会调用代理对象的MessageSendProxy.handleInvocation方法，
             * 然后在那里会收集类名、方法名、参数等信息，然后通过网络发送给服务端，进行远程过程调用。
             * 这种方法太巧妙了，真的在实现上使得rpc就像是本地调用一样，上层服务根本不知道调用最终的加法操作是在哪里进行的。
             */
            int add = calc.add(taskNumber, taskNumber);
            // System.out.println("calc add result:[ " + add + " ]");

            finish.countDown();
        } catch (InterruptedException ex) {
            Logger.getLogger(CalcParallelRequestThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
