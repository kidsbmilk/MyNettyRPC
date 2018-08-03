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

**基于Netty打造RPC服务器设计经验谈**
http://www.cnblogs.com/jietang/p/5983038.html

理解JMX之介绍和简单使用
https://blog.csdn.net/lmy86263/article/details/71037316

ApplicationContextAware得到ApplicationContext的原理
https://blog.csdn.net/xtj332/article/details/20127501

Spring afterPropertiesSet方法
https://blog.csdn.net/u013013553/article/details/79038702

在NettyRPC 2.0的基础上新增NettyRPC异步回调功能模块：
基于cglib生成异步代理Mock对象，针对一些极端耗时的RPC调用场景进行异步回调，从而提高客户端的并行吞吐量。

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











