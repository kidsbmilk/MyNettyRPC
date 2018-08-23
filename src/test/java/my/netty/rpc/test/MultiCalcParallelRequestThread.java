package my.netty.rpc.test;

import my.netty.rpc.exception.InvokeTimeoutException;
import my.netty.rpc.services.MultiCalculate;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiCalcParallelRequestThread implements Runnable {

    private CountDownLatch signal;
    private CountDownLatch finish;
    private int taskNumber = 0;
    private MultiCalculate calc;

    public MultiCalcParallelRequestThread(MultiCalculate calc, CountDownLatch signal, CountDownLatch finish, int taskNumber) {
        this.signal = signal;
        this.finish = finish;
        this.taskNumber = taskNumber;
        this.calc = calc;
    }

    public void run() {
        try {
            signal.await();
            int multi = calc.multi(taskNumber, taskNumber);
            System.out.println("calc multi result:[" + multi + "]");
        } catch (InterruptedException e) {
            Logger.getLogger(MultiCalcParallelRequestThread.class.getName()).log(Level.SEVERE, null, e);
        } catch (InvokeTimeoutException e) {
            System.out.println(e.getMessage());
        } finally {
            finish.countDown();
        }
    }
}
