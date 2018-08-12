package my.netty.rpc.test.jdbc;

import my.netty.rpc.services.JdbcPersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class NettyRpcJdbcClientTest {
    // FIXME: 2017/9/25 确保先启动NettyRPC服务端应用:NettyRpcJdbcServerTest，再运行NettyRpcJdbcClientTest、NettyRpcJdbcClientErrorTest！

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-jdbc-client.xml");

        JdbcPersonManage manage = (JdbcPersonManage) context.getBean("personManageJdbc");

        try {
            Person p = new Person();
//            p.setId(1);
            p.setName("world222");
            p.setAge(6);
            p.setBirthday(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-11 13:03:00"));
            int result = manage.save(p);
            manage.query(p); // 这个查找要指定准确的id以及其他三个参数才会成功的。
            System.out.println("call pojo rpc result: " + result);

            System.out.println("-----------------------------------");

            List<Person> list = manage.query(); // 这个查出表中所有记录。
            for(int i = 0; i < list.size(); i ++) {
                System.out.println(list.get(i));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            context.destroy();
        }
    }
}
