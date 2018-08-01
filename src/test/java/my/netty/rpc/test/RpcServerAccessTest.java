package my.netty.rpc.test;

import my.netty.rpc.compiler.AccessAdaptive;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcServerAccessTest {
    // TODO:
    // java source only support public method
    private static String CODE =
            "package my.netty.rpc.test;\n" +
                    "\n" +
                    "import java.text.SimpleDateFormat;\n" +
                    "import java.util.Date;\n" +
                    "\n" +
                    "public class RpcServerAccessProvider {\n" +
                    "   public String getRpcServerTime(String message) {\n" +
                    "       SimpleDateFormat df = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
                    "       return \"NettyRpc server receive:\" + message + \", server time is:\" + df.format(new Date());\n" +
                    "   }\n" +
                    "\n" +
                    "   public void sayHello() {\n" +
                    "       System.out.println(\"Hello NettyRpc!\");\n" +
                    "   }\n" +
                    "}";

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        AccessAdaptive provider = (AccessAdaptive) context.getBean("access");

        String result = (String) provider.invoke(CODE, "getRpcServerTime", new Object[]{new String("Soybeanmilk")});
        System.out.println(result);

        provider.invoke(CODE, "sayHello", new Object[0]);

        context.destroy();
    }
}
