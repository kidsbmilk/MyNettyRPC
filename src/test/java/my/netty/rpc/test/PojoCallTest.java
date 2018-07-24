package my.netty.rpc.test;

import my.netty.rpc.services.PersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PojoCallTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        PersonManage manage = (PersonManage) context.getBean("personManage");

        Person p = new Person();
        p.setId(666666);
        p.setName("test");
        p.setAge(22);

        int result = manage.save(p);

        System.out.println("call pojo rpc result: " + result);

        context.destroy();
    }
}
