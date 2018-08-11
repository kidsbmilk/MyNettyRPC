package my.netty.rpc.test;

import my.netty.rpc.exception.InvokeModuleException;
import my.netty.rpc.services.PersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PojoCallErrorTest {

    public static void test1(PersonManage manage) {
        try {
            manage.check();
        } catch (InvokeModuleException e) {
            // e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public static void test2(PersonManage manage) {
        try {
            Person p = new Person();
            p.setId(20180811);
            p.setName("hello");
            p.setAge(2);
            manage.checkAge(p);
        } catch (InvokeModuleException e) {
            // e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");
        PersonManage manage = (PersonManage) context.getBean("personManage");

        try {
            test1(manage);
            test2(manage);
        } finally {
            context.destroy();
        }
    }
}
