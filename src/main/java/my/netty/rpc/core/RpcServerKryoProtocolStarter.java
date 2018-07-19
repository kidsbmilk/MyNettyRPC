package my.netty.rpc.core;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerKryoProtocolStarter {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("rpc-invoke-config-kryo.xml");
    }
}
