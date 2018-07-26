package my.netty.rpc.test;

import my.netty.rpc.services.PersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PojoCallTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        PersonManage manage = (PersonManage) context.getBean("personManage"); // 注意：仅仅是返回一个对象（代理对象），并没有其他的动作。

        Person p = new Person();
        p.setId(666666);
        p.setName("test");
        p.setAge(22);

        int result = manage.save(p); // 这里才会调用代理对象MessageSendProxy的handleInvocation方法，进行远程过程调用，整个过程，对客户端来说，就像是在本地调用的一样，感觉很神奇。

        System.out.println("call pojo rpc result: " + result);

        context.destroy();
    }
}
