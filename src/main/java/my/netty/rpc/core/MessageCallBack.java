package my.netty.rpc.core;

import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rpc消息回调
 */
public class MessageCallBack {

    private MessageRequest request;
    private MessageResponse response;
    private Lock lock = new ReentrantLock();
    private Condition finish = lock.newCondition();

    public MessageCallBack(MessageRequest request) {
        this.request = request;
    }

    public Object start() throws InterruptedException {
        try {
            lock.lock();
            finish.await(10 * 1000, TimeUnit.MILLISECONDS);
            if(this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    public void over(MessageResponse response) { // over有结束的意思，在这里的意思是：远程过程调用有结果返回了，在这里开始设置调用成功后的结果，
        // 远程调用结束了，本地得到结果后的处理还没结束。
        try {
            lock.lock();
            finish.signal();
            this.response = response;
        } finally {
            lock.unlock();
        }
    }
}
