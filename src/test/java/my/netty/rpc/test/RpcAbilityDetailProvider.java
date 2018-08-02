package my.netty.rpc.test;

import my.netty.rpc.core.AbilityDetail;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcAbilityDetailProvider {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        AbilityDetail provider = (AbilityDetail) context.getBean("ability");

        StringBuilder ability = provider.listAbilityDetail();

        System.out.println(ability);

        context.destroy();
    }
}
