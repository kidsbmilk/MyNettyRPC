package my.netty.rpc.event.system.event;

import org.springframework.context.ApplicationEvent;

// 这个只在NettyRpcService中使用到了，用于通知添加新的服务。
public class ServerStartEvent extends ApplicationEvent {

    public ServerStartEvent(Object source) {
        super(source);
    }
}
