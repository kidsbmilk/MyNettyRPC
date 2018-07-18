package my.netty.rpc.core;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerJdkNativeProtocolStarter {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("my/netty/rpc/config/rpc-invoke-config-jdknative.xml");
    }
}
