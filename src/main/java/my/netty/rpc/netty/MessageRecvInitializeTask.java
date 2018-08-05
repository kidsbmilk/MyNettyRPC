package my.netty.rpc.netty;

import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;


import java.util.Map;
import java.util.concurrent.Callable;

public class MessageRecvInitializeTask implements Callable<Boolean> {

    private MessageRequest request = null;
    private MessageResponse response = null;
    private Map<String, Object> handlerMap = null;
    private static final String METHOD_MAPPED_NAME = "invoke";
    private boolean returnNotNull = true;

    MessageRecvInitializeTask(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
        this.request = request;
        this.response = response;
        this.handlerMap = handlerMap;
    }

    public Boolean call() {
        response.setMessageId(request.getMessageId());
        try {
            Object result = reflect(request); // 在这里服务器端处理客户端发来的调用请求。
            if((returnNotNull && result != null) || !returnNotNull) {
                response.setResult(result);
                response.setError("");
                response.setReturnNotNull(returnNotNull);
            } else {
                System.err.println(RpcSystemConfig.FILTER_RESPONSE_MSG);
                response.setResult(null);
                response.setError(RpcSystemConfig.FILTER_RESPONSE_MSG);
            }
            return Boolean.TRUE;
        } catch (Throwable t) {
            response.setError(t.toString());
            t.printStackTrace();
            System.err.printf("RPC Server invoke error!\n");
            return Boolean.FALSE;
        }
    }

    private Object reflect(MessageRequest request) throws Throwable {
//        String className = request.getClassName(); // 这两行代码移到MethodProxyAdvisor.invoke中了。
//        Object serviceBean = handlerMap.get(className);
//        String methodName = request.getMethodName(); // 这三行代码移到MethodInvoker.invoke中了。
//        Object[] parameters = request.getParametersVal();
//        return MethodUtils.invokeMethod(serviceBean, methodName, parameters); // 在这里服务器端处理客户端发来的调用请求。
        ProxyFactory weaver = new ProxyFactory(new MethodInvoker());
        NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor();
        advisor.setMappedName(METHOD_MAPPED_NAME);
        advisor.setAdvice(new MethodProxyAdvisor(handlerMap));
        weaver.addAdvisor(advisor);
        MethodInvoker mi = (MethodInvoker) weaver.getProxy();
        Object obj = mi.invoke(request);
        setReturnNotNull(((MethodProxyAdvisor) advisor.getAdvice()).isReturnNotNull());
        return obj;
    }

    public boolean isReturnNotNull() {
        return returnNotNull;
    }

    public void setReturnNotNull(boolean returnNotNull) {
        this.returnNotNull = returnNotNull;
    }

    public MessageResponse getResponse() {
        return response;
    }

    public MessageRequest getRequest() {
        return request;
    }

    public void setRequest(MessageRequest request) {
        this.request = request;
    }
}
