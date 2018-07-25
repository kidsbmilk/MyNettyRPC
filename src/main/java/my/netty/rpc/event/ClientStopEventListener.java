package my.netty.rpc.event;

import com.google.common.eventbus.Subscribe;
import my.netty.rpc.netty.MessageSendExecutor;

public class ClientStopEventListener {

    public String lastMessage = "";

    // Google EventBus 使用详解
    // https://blog.csdn.net/zhglance/article/details/54314823
    @Subscribe
    public void listen(ClientStopEvent event) {
        lastMessage = event.getMessage();
        System.out.println("end：" + lastMessage);
        MessageSendExecutor.getInstance().stop();
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
