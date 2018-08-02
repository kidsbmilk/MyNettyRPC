package my.netty.rpc.compiler;

import com.google.common.io.Files;
import my.netty.rpc.core.ReflectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class AccessAdaptiveProvider extends AbstractAccessAdaptive implements AccessAdaptive {

    @Override
    protected Class<?> doCompile(String clsName, String javaSource) {

        File tempFileLocation = Files.createTempDir();
        try (NativeCompiler compiler = new NativeCompiler(tempFileLocation)) {
            return compiler.compile(clsName, javaSource);
        }
    }

    @Override
    public Object invoke(String javaSource, String method, Object[] args) {
        if(StringUtils.isEmpty(javaSource) || StringUtils.isEmpty(method)) {
            return null;
        } else {
            try {
                ClassProxy main = new ClassProxy();
                Class<?> type = compile(javaSource, null);
                Class<?> objectClass = main.createDynamicSubclass(type);
                Object object = ReflectionUtils.newInstance(objectClass); // 参考AsyncCallResult.getResult中的注释。
                assert object != null;
                return MethodUtils.invokeMethod(object, method, args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
