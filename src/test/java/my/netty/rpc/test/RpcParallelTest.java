package my.netty.rpc.test;

import my.netty.rpc.services.AddCalculate;
import my.netty.rpc.services.MultiCalculate;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RpcParallelTest {

    public static void parallelAddCalcTask(AddCalculate calc, int parallel) throws InterruptedException {
        StopWatch sw = new StopWatch();
        sw.start();

        CountDownLatch signal = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(parallel);

        for(int index = 0; index < parallel; index ++) {
            AddCalcParallelRequestThread client = new AddCalcParallelRequestThread(calc, signal, finish, index);
            new Thread(client).start();
        }

        signal.countDown();
        finish.await();
        sw.stop();

        String tip = String.format("加法计算RPC调用总天耗时: [%s] 毫秒", sw.getTime());
        System.out.println(tip);
    }

    public static void parallelMultiCalcTask(MultiCalculate calc, int parallel) throws InterruptedException {
        StopWatch sw = new StopWatch();
        sw.start();

        CountDownLatch signal = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(parallel);

        for(int index = 0; index < parallel; index ++) {
            MultiCalcParallelRequestThread client = new MultiCalcParallelRequestThread(calc, signal, finish, index);
            new Thread(client).start();
        }

        signal.countDown();
        finish.await();
        sw.stop();

        String tip = String.format("乘法计算RPC调用总耗时: [%s] 毫秒", sw.getTime());
        System.out.println(tip);
    }

    public static void addTask(AddCalculate calc, int parallel) throws InterruptedException {
        RpcParallelTest.parallelAddCalcTask(calc, parallel);
        TimeUnit.MILLISECONDS.sleep(30);
    }

    public static void multiTask(MultiCalculate calc, int parallel) throws InterruptedException {
        RpcParallelTest.parallelMultiCalcTask(calc, parallel);
        TimeUnit.MILLISECONDS.sleep(30);
    }

    public static void main(String[] args) throws Exception {
        int parallel = 1000;
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:rpc-invoke-config-client.xml");
        // 疑问：这里只创建了一个客户端链接，而下面是多个线程都通过一个链接向远方发起调用请求，感觉实质上还是单线程的，有点像数据库连接池。 ?zz?
        // 验证一下上面的疑问是否正确。
        // 通过在MessageSendHandler.sendRequest里添加：System.out.println(channel.localAddress())，
        // 以及在MessageRecvHandler.channelRead里添加：System.out.println(ctx.channel().remoteAddress())，
        // 验证了上面的想法，确实是在一个连接里发起了2000次请求。
        // 但是，我在终端用netstat -nat | grep 18887观察建立的连接时，发现，存在4个连接，但是上面的操作只用了其中一个，那么这4个连接是在何时发起的呢？
        // 这4次是在NettyRpcReference.afterPropertiesSet中发起的，因为MessageSendExecutor、RpcServerLoader都是单例模式，所以尽管有4个连接，
        // 但是只会保存一个MessageSendExecutor.loader.messageSendHandler（可以在RpcServerLoader.setMessageSendHandler里下断点，
        // 可以发现有4次设置这个messageSendHandler，只保留最后一次设置的），所以这2000次请求，只会使用一个连接来发请求。
        // 这算是作者设计的有问题，可以改一下。
        // TODO-THIS.
        // 这里面涉及到的一些共享细节：多个连接共享一个EventLoopGroup（作者实现了），还有多个连接也可以共享同一个MessageSendHandler（作者没这样做）。
        // 对于共享MessageSendHandler，正确的做法如下：比如这里发起了4个连接，这4个连接共享一个MessageSendHandler时，要确保MessageSendHandler是线程安全的，
        // 这4个连接在共享一个MessageSendHandler时，依然是并发操作的。
        // 但是，在这里的实现中：并不知道作者的本意是什么，但是由于设计错误，导致4个连接，只保留了一份MessageSendExecutor.loader.messageSendHandler，
        // 在这2000次多线程并发请求时，只会使用一个连接来发请求，相当于还是只有一个单线程在发请求。
        // 我想作者的本意应该是保留4份MessageSendExecutor.loader.messageSendHandler，或者是4个连接共享一个MessageSendExecutor.loader.messageSendHandler。

        for(int i = 0; i < 1; i ++) {
            addTask((AddCalculate) context.getBean("addCalc"), parallel);
            multiTask((MultiCalculate) context.getBean("multiCalc"), parallel);
            System.out.printf("Netty RPC Server 消息协议序列化第[%d]轮并发验证结束!\n\n", i);
        }

        context.destroy();
    }
}
