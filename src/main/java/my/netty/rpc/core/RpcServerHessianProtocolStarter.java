package my.netty.rpc.core;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerHessianProtocolStarter {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("rpc-invoke-config-hessian.xml");
    }
}
