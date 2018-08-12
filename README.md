# MyNetty
high performance java rpc server base on netty framework, using kryo, hession support rpc message serialization. LEARN FROM ZERO.

# 此项目仅用于学习，原作者：https://github.com/tang-jie/NettyRPC 非常感谢原作者~


谈谈如何使用Netty开发实现高性能的RPC服务器 http://www.cnblogs.com/jietang/p/5615681.html

Netty实现高性能RPC服务器优化篇之消息序列化 http://www.cnblogs.com/jietang/p/5675171.html

## MyNettyRPC 1.0 中文简介：
**MyNettyRPC是基于Netty构建的RPC系统，消息网络传输支持目前主流的编码解码器**
* NettyRPC基于Java语言进行编写，网络通讯依赖Netty。
* RPC服务端采用线程池对RPC调用进行异步回调处理。
* 服务定义、实现，通过Spring容器进行加载、卸载。
* 消息网络传输除了JDK原生的对象序列化方式，还支持目前主流的编码解码器：kryo、hessian。
* Netty网络模型采用主从Reactor线程模型，提升RPC服务器并行吞吐性能。
* 多线程模型采用guava线程库进行封装。


## MyNettyRPC 2.0 中文简介：
**MyNettyRPC 2.0是基于MyNettyRPC 1.0 在Maven下构建的RPC系统，在原有1.0版本的基础上对代码进行重构升级，主要改进点如下：**
* RPC服务启动、注册、卸载支持通过Spring中的MyNettyRPC标签进行统一管理。
* 在原来编码解码器：JDK原生的对象序列化方式、kryo、hessian，新增了：protostuff。
* 优化了NettyRPC服务端的线程池模型，支持LinkedBlockingQueue、ArrayBlockingQueue、SynchronousQueue，并扩展了多个线程池任务处理策略。

在NettyRPC 2.0的基础上新增NettyRPC异步回调功能模块：
基于cglib生成异步代理Mock对象，针对一些极端耗时的RPC调用场景进行异步回调，从而提高客户端的并行吞吐量。

在2.1版本的基础上，提供NettyRPC服务端接口能力展现功能：
接口能力展现功能模块部署在服务端的18889端口，可以在浏览器中输入：http://ip地址:18889/NettyRPC.html 进行查看。

NettyRPC客户端支持重连功能：这点主要是针对RPC服务器宕机的情形下，RPC客户端可以检测链路情况，如果链路不通，则自动重连。重连重试的时间默认为10s。

新增NettyRPC过滤器功能：
* 进一步合理地分配和利用服务端的系统资源，NettyRPC可以针对某些特定的RPC请求，进行过滤拦截。
* 具体过滤器要实现：my.netty.rpc.filter.Filter接口定义的方法。
* 被拦截到的RPC请求，NettyRPC框架会抛出my.netty.rpc.exception.RejectResponeException异常，可以根据需要进行捕获。
* spring配置文件中的nettyrpc:service标签，新增filter属性，用来定义这个服务对应的过滤器的实现。当然，filter属性是可选的。

----------
## NettyRPC 2.4
增强了RPC服务端动态加载字节码时，对于热点方法的拦截判断能力：
* 在之前的NettyRPC版本中，RPC服务端集成了一个功能：针对Java HotSpot虚拟机的热加载特性，可以动态加载、生成并执行客户端的热点代码。然而却有一定的风险。因为这些代码中的某些方法，可能存在一些危及服务端安全的操作，所以有必要对这些方法进行拦截控制。
* 技术难点在于：如何对服务端生成的字节码文件进行渲染加工？以往传统的方式，都是基于类进行代理渲染，而这次是针对字节码文件进行织入渲染，最终把拦截方法织入原有的字节码文件中。
* 对字节码操作可选的方案有Byte Code Engineering Library (BCEL)、ASM等。最终从执行性能上考虑，决定采用偏向底层的ASM，对字节码进行渲染织入增强，以节省性能开销。最终通过类加载器，重新把渲染后的字节码，载入运行时上下文环境。
* 具体方法拦截器要实现：my.netty.rpc.compiler.intercept.Interceptor接口定义的方法。NettyRPC框架提供了一个简易的拦截器实现：SimpleMethodInterceptor，可以在这里加入你的拦截判断逻辑。

 

**基于Netty打造RPC服务器设计经验谈**
http://www.cnblogs.com/jietang/p/5983038.html

理解JMX之介绍和简单使用
https://blog.csdn.net/lmy86263/article/details/71037316

ApplicationContextAware得到ApplicationContext的原理
https://blog.csdn.net/xtj332/article/details/20127501

Spring afterPropertiesSet方法
https://blog.csdn.net/u013013553/article/details/79038702

TODO:
1、整体设计上，服务器端目前只能在启动的确定并设置一种序列化方式。
改进：其实可以将多个序列化方式实现的handler全部放在pipeline中，然后以某个字段来标识使用哪个来反序列化或者序列化。

2、见RpcParallelTest里的TODO-THIS里的说明。整体的客户端的结构要大改，组成类似于连接池的结构，
还可以区分不同的序列化方式的连接，连接池中各个连接的负载均衡之类的。

3、这个问题是结合上面的1、2的，如果客户端启动多个序列化方式，而客户端在发送的时候共用了一个连接，在发送时就会出错，
假如客户端发送成功了，而序列化方式与服务端不同，也会出问题。
目前运行的良好，是因为服务器端与客户端都使用的PROTOSTUFFSERIALIZE的序列化方式。

4、在RpcServerStart启动后，每次启动RpcParallelTest执行时发送1000次请求，大概率前几次启动测试时，客户端会卡一下，
有几个请求在客户端反应的特别慢，我看服务器端已经把结果返回去了。这个问题是最近在我这个版本中发现的，作者的版本中没有这个问题，
难道是我改了什么东西导致的吗？挨个测试历史版本吧，看看中哪里出问题了。
挨个对着文件查了一遍，改了几个地方，代码不那么 容易卡住了，但是偶尔还是会卡住，感觉还是没彻底解决这个问题，难道真的是正常的线程卡住吗？
# 这个还是有问题！可能是因为用错protostuff包的原因。
一种猜测：与RpcSystemConfig.SYSTEM_PROPERTY_ASYNC_MESSAGE_CALLBACK_TIMEOUT有关，我这里设置的是60s，而原作者设置的是10s，我把它设置为10s时，依然会卡住。

5、
如果依赖是以下的情况的话，console里会显示：ERROR StatusLogger No log4j2 configuration file found. Using default configuration: logging only errors to the console.
即：只显示error，不显示警告。
<dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.12</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>2.4.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.4.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
            <version>2.4.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-jcl</artifactId>
            <version>2.4.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-jul</artifactId>
            <version>2.4.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-web</artifactId>
            <version>2.4.1</version>
            <scope>runtime</scope>
        </dependency>

但是，如果显示警告级别的日志的话，会看到一个重要的警告：
警告: An exception was thrown by my.netty.rpc.netty.MessageRecvExecutor$2.operationComplete()
io.netty.util.concurrent.BlockingOperationException: AbstractChannel$CloseFuture@6c2a24ec(incomplete)
	at io.netty.util.concurrent.DefaultPromise.checkDeadLock(DefaultPromise.java:395)
	at io.netty.channel.DefaultChannelPromise.checkDeadLock(DefaultChannelPromise.java:159)
	at io.netty.util.concurrent.DefaultPromise.await(DefaultPromise.java:225)
	at io.netty.channel.DefaultChannelPromise.await(DefaultChannelPromise.java:131)
	at io.netty.channel.DefaultChannelPromise.await(DefaultChannelPromise.java:30)
	at io.netty.util.concurrent.DefaultPromise.sync(DefaultPromise.java:337)
	at io.netty.channel.DefaultChannelPromise.sync(DefaultChannelPromise.java:119)
	at io.netty.channel.DefaultChannelPromise.sync(DefaultChannelPromise.java:30)
	at my.netty.rpc.netty.MessageRecvExecutor$2.operationComplete(MessageRecvExecutor.java:118)
	at my.netty.rpc.netty.MessageRecvExecutor$2.operationComplete(MessageRecvExecutor.java:110)
	at io.netty.util.concurrent.DefaultPromise.notifyListener0(DefaultPromise.java:511)
	at io.netty.util.concurrent.DefaultPromise.notifyListeners0(DefaultPromise.java:504)
	at io.netty.util.concurrent.DefaultPromise.notifyListenersNow(DefaultPromise.java:483)
	at io.netty.util.concurrent.DefaultPromise.notifyListeners(DefaultPromise.java:424)
	at io.netty.util.concurrent.DefaultPromise.addListener(DefaultPromise.java:162)
	at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:95)
	at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:30)
	at io.netty.bootstrap.AbstractBootstrap$2.run(AbstractBootstrap.java:366)
	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:163)
	at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:404)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:463)
	at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:886)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.lang.Thread.run(Thread.java:748)

分析 Netty 死锁异常 BlockingOperationException
http://www.linkedkeeper.com/detail/blog.action?bid=1027&utm_medium=hao.caibaojian.com&utm_source=hao.caibaojian.com
这个文章太好了！
此问题已解决。

6、MessageRecvInitializeTask.reflect中的注释。

7、线程分析：
服务器端：
主线程
boss
worker
threadPoolExecutor(见MessageRecvExecutor.submit，多个任务共用一个线程池，可处理阻塞任务)

客户端：
主线程
eventLoopGroup
threadPoolExecutor（见RpcServerLoader.load，用于发起多个连接，最终连接都绑定到eventLoopGroup中的线程上）
executor（见AsyncInvoker.executor，用于客户端向服务器端提交异步执行的任务）

服务器端耗时任务是在threadPoolExecutor中执行的，而客户端对于非异步的且是阻塞执行的任务，阻塞发生在主线程中。

对于客户端，在发起连接后，threadPoolExecutor就会闲置，不如将executor也替换为threadPoolExecutor，这样可以减小客户端的线程数，节约资源。











