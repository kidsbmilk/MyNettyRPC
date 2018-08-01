package my.netty.rpc.compiler;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;

import java.util.List;

public class ClassProxy {

    public <T> Class<T> createDynamicSubclass(Class<T> superClass) {
        Enhancer enhancer = new Enhancer() {
            @Override
            protected void filterConstructors(Class sc, List constructors) {
                // FIXME:
                // maybe change javassist support
            }
        };

        if(superClass.isInterface()) {
            enhancer.setInterfaces(new Class[]{superClass});
        } else {
            enhancer.setSuperclass(superClass);
        }

        enhancer.setCallbackType(NoOp.class);
        Class<T> proxyClass = enhancer.createClass();
        return proxyClass;
    }
}
