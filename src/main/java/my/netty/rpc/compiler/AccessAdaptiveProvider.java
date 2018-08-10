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
                Object object = ReflectionUtils.newInstance(type); // 这个对象是编译用户发送来的java代码后创建的类的对象，是原始的对象，
                // 现在要在原始的对象的基础上，操作字节码，达到拦截方法调用的目的。
                Thread.currentThread().getContextClassLoader().loadClass(type.getName()); // 将类加载到当前线程的上下文加载器中。
                Object proxy = getProxyFactory().createProxy(object, new SimpleMethodInterceptor(), new Class[]{type}); // 这行代码在原来框架的基础上，把asm方法的拦截代码添加到框架里了，
                // 增加asm相关代码的目的：增强了RPC服务端动态加载字节码时，对于热点方法的拦截判断能力，见README.md。
                // 虽然上面创建经asm操作后的类的对象时传递的是Interceptor对象，但其实内部实现时，类的构造函数的参数是ObjectInvoker类型的，有点迷惑性。
                return MethodUtils.invokeMethod(proxy, method, args);
                // 调用流程如下：如果没有经过asm编织，那么上面的调用直接就是调用用户代码编译后的类的method方法，
                // 在经过编织后，得到的是用户代码编译后的类的子类，这个子类在调用用户代码时做一些其他工作，在调用method时，这个子类对象proxy会调用ObjectInvoker的方法，
                // 具体是InterceptorInvoker的invoke方法，然后InterceptorInvoker.invokeImpl会将调用转到SimpleMethodInterceptor.intercept里，
                // 最终调用InvocationProvider.proceed()，然后就可以看到method.invoke(target, arguments)了，最终的method发生了。
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
