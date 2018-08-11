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
            p.setId(1);
            p.setName("world");
            p.setAge(2);
            p.setBirthday(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-11 13:03:00"));
            int result = manage.save(p);
            manage.query(p);
            System.out.println("call pojo rpc result: " + result);

            System.out.println("-----------------------------------");

            List<Person> list = manage.query();
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
