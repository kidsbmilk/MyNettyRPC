package my.netty.rpc.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;

public class MessageRequest implements Serializable {

    private String messageId;
    private String className;
    private String methodName;
    private Class<?>[] typeParameters;
    private Object[] parametersVal;
    private boolean invokeMetrics = true;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(Class<?>[] typeParameters) {
        this.typeParameters = typeParameters;
    }

    public Object[] getParametersVal() {
        return parametersVal;
    }

    public void setParametersVal(Object[] parametersVal) {
        this.parametersVal = parametersVal;
    }

    public boolean isInvokeMetrics() {
        return invokeMetrics;
    }

    public void setInvokeMetrics(boolean invokeMetrics) {
        this.invokeMetrics = invokeMetrics;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringExclude(this, new String[]{"typeParameters", "parametersVal"});
    }
}
