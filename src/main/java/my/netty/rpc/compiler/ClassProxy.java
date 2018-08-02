package my.netty.rpc.compiler;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;

import java.util.List;

public class ClassProxy {

    public <T> Class createDynamicSubclass(Class<T> superClass) {
        Enhancer enhancer = new Enhancer() {
            @Override
            protected void filterConstructors(Class sc, List constructors) { // 见包中的类注释，默认实现是过滤掉私有的构造器，这里是空实现，即是不保留所有构造器。
                // FIXME:
                // maybe change javassist support
            }
        };

        if(superClass.isInterface()) {
            enhancer.setInterfaces(new Class[]{superClass});
        } else {
            enhancer.setSuperclass(superClass);
        }

        enhancer.setCallbackType(NoOp.class); // 见包中的类注释。
        return enhancer.createClass();
    }
}
