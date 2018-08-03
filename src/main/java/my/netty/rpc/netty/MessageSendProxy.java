package my.netty.rpc.netty;

import com.google.common.reflect.AbstractInvocationHandler;
import my.netty.rpc.core.MessageCallBack;
import my.netty.rpc.model.MessageRequest;

import java.lang.reflect.Method;
import java.util.UUID;

public class MessageSendProxy<T> extends AbstractInvocationHandler { // 见MessageSendExecutor中的分析。
//public class MessageSendProxy extends AbstractInvocationHandler {

    public Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
        MessageRequest request = new MessageRequest();
        request.setMessageId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setTypeParameters(method.getParameterTypes());
        request.setParameters(args);

        MessageSendHandler handler = RpcServerLoader.getInstance().getMessageSendHandler();
        MessageCallBack callBack = handler.sendRequest(request); // 远程调用已开始
        return callBack.start(); // 这里阻塞等待远程调用返回结果
    }
}
