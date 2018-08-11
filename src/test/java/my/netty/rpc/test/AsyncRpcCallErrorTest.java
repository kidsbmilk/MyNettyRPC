package my.netty.rpc.test;

import my.netty.rpc.async.AsyncCallObject;
import my.netty.rpc.async.AsyncCallback;
import my.netty.rpc.async.AsyncInvoker;
import my.netty.rpc.exception.AsyncCallException;
import my.netty.rpc.services.CostTimeCalculate;
import my.netty.rpc.services.pojo.CostTime;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AsyncRpcCallErrorTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");
        final CostTimeCalculate calculate = (CostTimeCalculate) context.getBean("costTime");
        AsyncInvoker invoker = new AsyncInvoker();
        try {
            CostTime elapse0 = invoker.submit(new AsyncCallback<CostTime>() {
                @Override
                public CostTime call() {
                    throw new RuntimeException("callculate fail 1!");
                }
            });

            System.out.println("1 async nettyrpc call:[" + "result:" + elapse0 + ", status:[" + ((AsyncCallObject) elapse0)._getStatus() + "]");
        } catch (AsyncCallException e) {
            System.out.println(e.getMessage());
            context.destroy();
            return;
        }

        context.destroy();
    }
}
