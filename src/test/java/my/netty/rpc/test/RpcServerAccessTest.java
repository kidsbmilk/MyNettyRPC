package my.netty.rpc.test;

import com.google.common.io.CharStreams;
import my.netty.rpc.compiler.AccessAdaptive;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class RpcServerAccessTest {
    public static void main(String[] args) {
        try {
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            Reader reader = new InputStreamReader(resourceLoader.getResource("AccessProvider.tpl").getInputStream(), "UTF-8");
            String javaSource = CharStreams.toString(reader);

            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

            AccessAdaptive provider = (AccessAdaptive) context.getBean("access");

            String result = (String) provider.invoke(javaSource, "getRpcServerTime", new Object[]{new String("Soybeanmilk")});
            System.out.println(result);

            provider.invoke(javaSource, "sayHello", new Object[0]);

            reader.close();
            context.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
