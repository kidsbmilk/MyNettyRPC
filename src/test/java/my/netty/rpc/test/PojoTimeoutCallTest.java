package my.netty.rpc.test;

import my.netty.rpc.exception.InvokeTimeoutException;
import my.netty.rpc.services.PersonManage;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PojoTimeoutCallTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        PersonManage manage = (PersonManage) context.getBean("personManage");

        // TODO-THIS:
        // NettyRPC default timeout is 30s. you can define it by nettyrpc.default.msg.timeout environment variable.
        // if rpc call timeout, NettyRPC can throw InvokeTimeoutException.
        try {
            long timeout = 32L;
            manage.query(timeout);
        } catch (InvokeTimeoutException e) {
            System.out.println(e.getMessage());
        } finally {
            context.destroy();
        }
    }
}
