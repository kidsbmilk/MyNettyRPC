package my.netty.rpc.event.system.event;

// 这个ServerStartEvent相对的，ServerStartEvent是服务器端启动时的事件，通过spring框架的消息通信机制做的，而这个ClientStopEvent则是客户端在退出时关闭服务接口用的，
// 是通过Google EventBus做的。
public class ClientStopEvent {

    private final int message;

    public ClientStopEvent(int message) {
        this.message = message;
    }

    public int getMessage() {
        return message;
    }
}
