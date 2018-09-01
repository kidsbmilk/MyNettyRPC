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

public abstract class AbstractMessageRecvInitializeTask implements Callable<Boolean> {

    protected MessageRequest request = null;
    protected MessageResponse response = null;
    protected Map<String, Object> handlerMap = null;
    protected static final String METHOD_MAPPED_NAME = "invoke";
    protected boolean returnNotNull = true;
    protected long invokeTimespan;

    public AbstractMessageRecvInitializeTask(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
        this.request = request;
        this.response = response;
        this.handlerMap = handlerMap;
    }

    @Override
    public Boolean call() {
        try {
            acquire();
            /**
             * ModuleMetricsVisitor是最终存统计数据的地方，见ModuleMetricsVisitor类注释，
             *
             * AbstractModuleMetricsHandler.getVisitor方法之所以使用并发措施（ConcurrentLinkedQueue），是因为可能有不同方法的不同统计任务在修改AbstractModuleMetricsHandler.visitorList。
             * （之前对这个ConcurrentLinkedQueue理解的不够：
             * 并发队列-无界非阻塞队列 ConcurrentLinkedQueue 原理探究：http://www.importnew.com/25668.html
             * 聊聊并发（六）ConcurrentLinkedQueue的实现原理分析：http://ifeve.com/concurrentlinkedqueue/
             *
             * 作者的用法好奇怪，得分析一下。TODO-THIS.
             * ）
             *
             * AbstractMessageRecvInitializeTask.call()方法之所以使用acquire以及release()（其实是全局锁），是因为防止相同方法的不同统计任务会把同一个visitor改乱了（不同方法的不同统计任务
             * 修改的是不同的visitor，所以不会乱掉）。
             *
             * 作者能考虑到上面两种并发，真是太厉害了。
             *
             * 之前的分析有点偏差。
             *
             * 而后面的HashMessageRecvInitializeTask的分块临界区，也是为了解决上面问题。
             * （真是道理都是相通的，要弄明白问题出在哪里，带着问题去学习，有问题才能知道自己究竟会不会、会什么。）
             */
            response.setMessageId(request.getMessageId());
            injectInvoke();
            Object result = reflect(request); // 在这里服务器端处理客户端发来的调用请求。
            if(!returnNotNull || result != null) {
                response.setResult(result);
                response.setError("");
                response.setReturnNotNull(returnNotNull);
                injectSuccInvoke(invokeTimespan);
            } else {
                System.err.println(RpcSystemConfig.FILTER_RESPONSE_MSG);
                response.setResult(null);
                response.setError(RpcSystemConfig.FILTER_RESPONSE_MSG);
                injectFilterInvoke();
            }
            return Boolean.TRUE;
        } catch (Throwable t) {
            response.setError(getStackTrace(t));
            t.printStackTrace();
            System.err.printf("RPC Server invoke error!\n");
            injectFailInvoke(t);
            return Boolean.FALSE;
        } finally {
            release();
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
        advisor.setAdvice(new MethodProxyAdvisor(handlerMap)); // 见MethodProxyAdvisor以及MethodInvoker里的注释。
        weaver.addAdvisor(advisor);
        MethodInvoker mi = (MethodInvoker) weaver.getProxy();
        Object obj = mi.invoke(request); // AccessAdaptive服务是在MessageRecvExecutor.register手动注册的，这里将AccessAdaptiveProvider与原有框架结合起来，
        invokeTimespan = mi.getInvokeTimespan();
        // 具体的MethodInvoker.serviceBean的设置是在MethodProxyAdvisor.invoke中设置的。
        setReturnNotNull(((MethodProxyAdvisor) advisor.getAdvice()).isReturnNotNull());
        return obj;
    }

    // StringWriter/PrintWriter在Java异常中的作用
    // https://blog.csdn.net/ititii/article/details/80502220
    // 异常类的toString()、getMessage()和printStackTrace()方法
    // https://blog.csdn.net/qq_15087157/article/details/78051400
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

    protected abstract void injectInvoke();

    protected abstract void injectSuccInvoke(long invokeTimespan);

    protected abstract void injectFailInvoke(Throwable error);

    protected abstract void injectFilterInvoke();

    protected abstract void acquire();

    protected abstract void release();
}
