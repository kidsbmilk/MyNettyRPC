package my.netty.rpc.netty;

import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;


import java.io.PrintWriter;
import java.io.StringWriter;
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
            response.setError(getStackTrace(t));
            t.printStackTrace();
            System.err.printf("RPC Server invoke error!\n");
            return Boolean.FALSE;
        }
    }

    /**
     * 注意：虽然这里用到了aop，但是并不是切面编程，而仅仅是把它当作代理来用了。拦截invoke方法，而这里所有调用都是通过invoke发生的，也就拦截了所有调用，
     * 但是，又在拦截后判断是否有过滤器，感觉多此一举，而且对于没有过滤器的调用操作多了一层，会导致性能损失。
     * TODO: 不如只用aop拦截有过滤器的调用操作，而没有过滤器的调用操作还是正常的发生调用过程。
     *
     * springboot aop简单示例
     * https://blog.csdn.net/bombsklk/article/details/79143145
     *
     * Spring boot中使用aop详解
     * https://www.cnblogs.com/bigben0123/p/7779357.html
     */
    private Object reflect(MessageRequest request) throws Throwable {
        // 对下面五行代码的重新安排是为了实现aop的过滤功能
//        String className = request.getClassName(); // 这两行代码移到MethodProxyAdvisor.invoke中了。
//        Object serviceBean = handlerMap.get(className);
//        String methodName = request.getMethodName(); // 这三行代码移到MethodInvoker.invoke中了。
//        Object[] parameters = request.getParametersVal();
//        return MethodUtils.invokeMethod(serviceBean, methodName, parameters); // 在这里服务器端处理客户端发来的调用请求。
        ProxyFactory weaver = new ProxyFactory(new MethodInvoker()); // 这里的aop主要用于实现过滤，见MethodProxyAdvisor.invoke的实现。
        // ProxyFactory是aop框架里的接口。
        NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor();
        advisor.setMappedName(METHOD_MAPPED_NAME);
        advisor.setAdvice(new MethodProxyAdvisor(handlerMap));
        weaver.addAdvisor(advisor);
        MethodInvoker mi = (MethodInvoker) weaver.getProxy();
        Object obj = mi.invoke(request); // AccessAdaptive服务是在MessageRecvExecutor.register手动注册的，这里将AccessAdaptiveProvider与原有框架结合起来，
        // 具体的MethodInvoker.serviceBean的设置是在MethodProxyAdvisor.invoke中设置的。
        setReturnNotNull(((MethodProxyAdvisor) advisor.getAdvice()).isReturnNotNull());
        return obj;
    }

    public String getStackTrace(Throwable e) {
        StringWriter buf = new StringWriter();
        e.printStackTrace(new PrintWriter(buf));

        return buf.toString();
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
