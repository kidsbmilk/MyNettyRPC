package my.netty.rpc.test;

import my.netty.rpc.exception.InvokeModuleException;
import my.netty.rpc.exception.InvokeTimeoutException;
import my.netty.rpc.services.AddCalculate;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddCalcParallelRequestThread implements Runnable {

    private CountDownLatch signal;
    private CountDownLatch finish;
    private int taskNumber = 0;
    private AddCalculate calc;

    public AddCalcParallelRequestThread(AddCalculate calc, CountDownLatch signal, CountDownLatch finish, int taskNumber) {
        this.calc = calc;
        this.signal = signal;
        this.finish = finish;
        this.taskNumber = taskNumber;
    }

    public void run() {
        try {
            signal.await();

            int add = calc.add(taskNumber, taskNumber);
            System.out.println("calc add result:[" + add + "]");
        } catch (InterruptedException e) {
            Logger.getLogger(AddCalcParallelRequestThread.class.getName()).log(Level.SEVERE, null, e);
        } catch (InvokeTimeoutException e) {
            System.out.println(e.getMessage());
        } finally {
            finish.countDown();
        }
    }
}
