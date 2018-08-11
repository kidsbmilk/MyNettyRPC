package my.netty.rpc.test.jdbc;

import my.netty.rpc.exception.InvokeModuleException;
import my.netty.rpc.services.JdbcPersonManage;
import my.netty.rpc.services.pojo.Person;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NettyRpcJdbcClientErrorTest {
    // FIXME: 2017/9/25 确保先启动NettyRPC服务端应用:NettyRpcJdbcServerTest，再运行NettyRpcJdbcClientTest、NettyRpcJdbcClientErrorTest！

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-jdbc-client.xml");

        JdbcPersonManage manage = (JdbcPersonManage) context.getBean("personManageJdbc");

        // 验证RPC调用服务器端执行失败的情况！
        Person p = new Person();
        p.setId(20180811);
        p.setName("hello");
        p.setAge(9999);

        try {
            int result = manage.save(p);
            System.out.println("call pojo rpc result: " + result);
        } catch (InvokeModuleException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            context.destroy();
        }
    }
}
