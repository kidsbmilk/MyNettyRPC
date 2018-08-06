package my.netty.rpc.compiler;

import com.google.common.io.Files;
import my.netty.rpc.compiler.intercept.SimpleMethodInterceptor;
import my.netty.rpc.core.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

// 这个类的实例在MessageRecvExecutor.register中被添加到handlerMap中。
public class AccessAdaptiveProvider extends AbstractAccessAdaptive implements AccessAdaptive {

    @Override
    protected Class<?> doCompile(String clsName, String javaSource) {

        File tempFileLocation = Files.createTempDir();
        compiler = new NativeCompiler(tempFileLocation); // 在父类AbstractAccessAdaptive中定义，也在父类的方法中关闭。
        Class type = compiler.compile(clsName, javaSource);
        tempFileLocation.deleteOnExit();
        return type;
    }

    @Override
    public Object invoke(String javaSource, String method, Object[] args) {
        if(StringUtils.isEmpty(javaSource) || StringUtils.isEmpty(method)) {
            return null;
        } else {
            try {
                Class type = compile(javaSource, Thread.currentThread().getContextClassLoader());
                Object object = ReflectionUtils.newInstance(type);
                Thread.currentThread().getContextClassLoader().loadClass(type.getName());
                Object proxy = getFactory().createProxy(object, new SimpleMethodInterceptor(), new Class[]{type});
                return MethodUtils.invokeMethod(proxy, method, args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
