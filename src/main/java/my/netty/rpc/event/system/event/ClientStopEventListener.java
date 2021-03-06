package my.netty.rpc.event.system.event;

import com.google.common.eventbus.Subscribe;
import my.netty.rpc.netty.MessageSendExecutor;

// 见ClientStopEvent里的类注释。
public class ClientStopEventListener {

    public int lastMessage = 0;

    // Google EventBus 使用详解
    // https://blog.csdn.net/zhglance/article/details/54314823
    @Subscribe
    public void listen(ClientStopEvent event) {
        lastMessage = event.getMessage();
//        System.out.println("end：" + lastMessage);
        MessageSendExecutor.getInstance().stop();
    }

    public int getLastMessage() {
        return lastMessage;
    }
}
