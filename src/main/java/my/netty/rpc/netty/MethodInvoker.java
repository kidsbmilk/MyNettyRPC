package my.netty.rpc.netty;

import my.netty.rpc.model.MessageRequest;
import org.apache.commons.lang3.reflect.MethodUtils;

public class MethodInvoker {

    private Object serviceBean;

    public Object getServiceBean() {
        return serviceBean;
    }

    public void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public Object invoke(MessageRequest request) throws Throwable {
        String methodName = request.getMethodName();
        Object[] parameters = request.getParametersVal();
        return MethodUtils.invokeMethod(serviceBean, methodName, parameters);
    }
}
