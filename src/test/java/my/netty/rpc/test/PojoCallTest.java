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
        // 疑问：manage.save经过远程过程调用、反序列化啥的，返回的应该是Object，为什么可以直接赋给result呢？是在哪里做了类型转换吗？
        // google Guava包的reflection解析
        // https://yq.aliyun.com/articles/20560
        /**
         * JDK动态代理需要类型转换：
         * Foo foo = (Foo) Proxy.newProxyInstance(
         * Foo.class.getClassLoader(),
         * new Class<?>[] {Foo.class},
         * invocationHandler);
         *
         * Guava不需要类型转换：
         * Foo foo = Reflection.newProxy(Foo.class, invocationHandler);
         *
         * 但是，内部原理还是不太明白？
         * 是我对动态代理不了解的原因，其实，JDK的动态代理在方法返回具体类型值时也是不需要进行类型转换的，可以写个JDK动态代理返回具体类型值的方法试一下。
         * 我猜测这里不需要转换的原因是：PersonManage manage或者Foo foo在获取动态代理对象时已经转换过代理对象类型了，然后在调用其他方法时，其实就跟直接调用原对象是一样的，所以就不需要转换返回值的类型了。
         * 具体实现就得看jvm源码了。
         */

        Person p2 = manage.getPerson(p);
        System.out.println(p2);

        System.out.println("call pojo rpc result: " + result);

        context.destroy();
    }
}
