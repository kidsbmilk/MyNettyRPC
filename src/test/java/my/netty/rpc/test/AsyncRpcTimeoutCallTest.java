package my.netty.rpc.test;

import my.netty.rpc.async.AsyncCallObject;
import my.netty.rpc.async.AsyncCallback;
import my.netty.rpc.async.AsyncInvoker;
import my.netty.rpc.exception.InvokeTimeoutException;
import my.netty.rpc.services.CostTimeCalculate;
import my.netty.rpc.services.pojo.CostTime;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AsyncRpcTimeoutCallTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        final CostTimeCalculate calculate = (CostTimeCalculate) context.getBean("costTime");

        AsyncInvoker invoker = new AsyncInvoker();

        try {
            CostTime elapse0 = invoker.submit(new AsyncCallback<CostTime>() {
                @Override
                public CostTime call() {
                    return calculate.busy();
                }
            });

            System.out.println("1 async nettyrpc fail:[" + "result:" + elapse0 + ", status:[" + ((AsyncCallObject) elapse0)._getStatus() + "]");
        } catch (InvokeTimeoutException e) {
            System.out.println(e.getMessage());
            context.destroy();
            return;
        }

        context.destroy();
    }
}
