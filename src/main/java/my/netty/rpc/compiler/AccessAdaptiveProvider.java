package my.netty.rpc.compiler;

import com.google.common.io.Files;
import my.netty.rpc.core.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class AccessAdaptiveProvider extends AbstractAccessAdaptive implements AccessAdaptive {

    @Override
    protected Class<?> doCompile(String clsName, String javaSource) throws Throwable {
        NativeCompiler compiler = null;

        try {
            File tempFileLocation = Files.createTempDir();
            compiler = new NativeCompiler(tempFileLocation);
            Class type = compiler.compile(clsName, javaSource);
            return type;
        } finally {
            compiler.close();
        }
    }

    @Override
    public Object invoke(String javaSource, String method, Object[] args) {
        if(StringUtils.isEmpty(javaSource) || StringUtils.isEmpty(method)) {
            return null;
        } else {
            try {
                ClassProxy main = new ClassProxy();
                Class type = compile(javaSource, null);
                Class<?> objectClass = main.createDynamicSubclass(type);
                Object object = ReflectionUtils.newInstance(objectClass);
                return MethodUtils.invokeMethod(object, method, args);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
