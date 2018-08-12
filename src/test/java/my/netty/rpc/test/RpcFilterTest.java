package my.netty.rpc.test;

import my.netty.rpc.exception.RejectResponseException;
import my.netty.rpc.services.Cache;
import my.netty.rpc.services.Store;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RpcFilterTest {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");

        Cache cache = (Cache) context.getBean("cache");

        for(int i = 0; i < 100; i ++) {
            String obj = String.valueOf(i);
            try {
                cache.put(obj, obj);
            } catch (RejectResponseException e) {
                System.out.println("trace: " + e.getMessage());
            }
        }

        for(int i = 0; i < 100; i ++) {
            String obj = String.valueOf(i);
            try {
                System.out.println((String) cache.get(obj));
            } catch (RejectResponseException e) {
                System.out.println("trace: " + e.getMessage());
            }
        }

        Store store = (Store) context.getBean("store");

        for(int i = 0; i < 100; i ++) {
            String obj = String.valueOf(i);
            try {
                store.save(obj);
                store.save(i);
            } catch (RejectResponseException e) {
                System.out.println("trace: " + e.getMessage());
            }
        }

        context.destroy();
    }
}
