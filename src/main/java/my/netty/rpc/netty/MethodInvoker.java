package my.netty.rpc.netty;

import my.netty.rpc.model.MessageRequest;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.time.StopWatch;

public class MethodInvoker {

    private Object serviceBean;
    private StopWatch sw = new StopWatch(); // 见org.apache.commons.lang3.time.StopWatch类注释

    public Object getServiceBean() {
        return serviceBean;
    }

    public void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public Object invoke(MessageRequest request) throws Throwable {
        String methodName = request.getMethodName();
        Object[] parameters = request.getParametersVal();
        sw.reset();
        sw.start();
        Object result = MethodUtils.invokeMethod(serviceBean, methodName, parameters); // 这个serviceBean的设置是在MethodProxyAdvisor.invoke里设置的，
        // 而MethodProxyAdvisor.invoke的调用时机是在AbstractMessageRecvInitializeTask.reflect里通过aop的切面拦截调用的。

        // 到这里时，serviceBean是去除filter之后的了，所以统计的sw.getTime()只是操作本身的时间，并不包含filter，以及其他的操作的时间，还是比较准确的。
        sw.stop();
        return result;
    }

    public long getInvokeTimespan() {
        return sw.getTime();
    }
}
