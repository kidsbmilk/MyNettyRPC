package my.netty.rpc.core;

import my.netty.rpc.exception.InvokeModuleException;
import my.netty.rpc.exception.InvokeTimeoutException;
import my.netty.rpc.exception.RejectResponseException;
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

    public Object start() {
        try {
            lock.lock();
            await();
            if(this.response != null) {
                if (getInvokeResult()) {
                    if(this.response.getError().isEmpty()) {
                        return this.response.getResult();
                    } else {
                        throw new InvokeModuleException(this.response.getError());
                    }
                } else {
                    throw new RejectResponseException(RpcSystemConfig.FILTER_RESPONSE_MSG);
                }
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

    private boolean getInvokeResult() {
        return !this.response.getError().equals(RpcSystemConfig.FILTER_RESPONSE_MSG)
                && (!this.response.isReturnNotNull() ||
                (this.response.isReturnNotNull() && this.response.getResult() != null));
    }

    private void await() {
        boolean timeout = false;
        try {
            timeout = finish.await(RpcSystemConfig.SYSTEM_PROPERTY_ASYNC_MESSAGE_CALLBACK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!timeout) {
            throw new InvokeTimeoutException(RpcSystemConfig.TIMEOUT_RESPONSE_MSG);
        }
    }
}
