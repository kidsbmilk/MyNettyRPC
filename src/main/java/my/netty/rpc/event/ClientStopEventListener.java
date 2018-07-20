package my.netty.rpc.event;

import com.google.common.eventbus.Subscribe;
import my.netty.rpc.netty.MessageSendExecutor;

public class ClientStopEventListener {

    public int lastMessage = 0;

    @Subscribe
    public void listen(ClientStopEvent event) {
        lastMessage = event.getMessage();
        MessageSendExecutor.getInstance().stop();
    }

    public int getLastMessage() {
        return lastMessage;
    }
}
