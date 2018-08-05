package my.netty.rpc.netty;

import my.netty.rpc.filter.Filter;
import my.netty.rpc.filter.ServiceFilterBinder;
import my.netty.rpc.model.MessageRequest;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.Map;

public class MethodProxyAdvisor implements MethodInterceptor {

    private Map<String, Object> handlerMap;

    private boolean returnNotNull = true;

    public boolean isReturnNotNull() {
        return returnNotNull;
    }

    public void setReturnNotNull(boolean returnNotNull) {
        this.returnNotNull = returnNotNull;
    }

    public MethodProxyAdvisor(Map<String, Object> handlerMap) {
        this.handlerMap = handlerMap;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object[] params = invocation.getArguments();
        if(params.length <= 0) return null;

        MessageRequest request = (MessageRequest) params[0];

        String className = request.getClassName();
        Object serviceBean = handlerMap.get(className);
        String methodName = request.getMethodName();
        Object[] parameters = request.getParametersVal();

        boolean existFilter = ServiceFilterBinder.class.isAssignableFrom(serviceBean.getClass());
        ((MethodInvoker) invocation.getThis()).setServiceBean(existFilter ? ((ServiceFilterBinder) serviceBean).getObject() : serviceBean);

        if(existFilter) {
            ServiceFilterBinder procesors = (ServiceFilterBinder) serviceBean;
            if(procesors.getFilter() != null) {
                Filter filter = procesors.getFilter();
                Object[] args = ArrayUtils.nullToEmpty(parameters);
                Class<?>[] parameterTypes = ClassUtils.toClass(args);
                Method method = MethodUtils.getMatchingAccessibleMethod(procesors.getObject().getClass(), methodName, parameterTypes);
                if(filter.before(method, procesors.getObject(), parameters)) {
                    Object result = invocation.proceed(); // 看这个方法的文档说明。
                    filter.after(method, procesors.getObject(), parameters);
                    setReturnNotNull(result != null);
                    return result;
                } else {
                    return null;
                }
            }
        }

        Object result = invocation.proceed();
        setReturnNotNull(result != null);
        return result;
    }
}
