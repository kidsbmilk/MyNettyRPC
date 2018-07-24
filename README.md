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

