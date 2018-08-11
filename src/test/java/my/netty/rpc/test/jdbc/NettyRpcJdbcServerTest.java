package my.netty.rpc.test.jdbc;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class NettyRpcJdbcServerTest {
    // FIXME: 2017/9/25 确保先启动NettyRPC服务端应用:NettyRpcJdbcServerTest，再运行NettyRpcJdbcClientTest、NettyRpcJdbcClientErrorTest！

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-jdbc-server.xml");
    }
    // 注意：这个例子要想运行，需要做以下处理：
    // 在MessageRecvExecutor.start()里启动ApiEchoResolver时，分析了两个步骤的异步与同步写法，如果两个步骤都用同步的写法，
    // 在我的mackbook air（cpu较弱，核数少，可能是此原因)会卡住，导致此例子运行失败，如果将两个步骤都用异步的写法，则此例子会运行成功，
    // 其他组合情况可自行尝试下。
}
