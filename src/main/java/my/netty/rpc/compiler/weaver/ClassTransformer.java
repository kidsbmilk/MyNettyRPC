package my.netty.rpc.compiler.weaver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class ClassTransformer implements Transformer {

    @Override
    public Class<?> transform(ClassLoader classLoader, Class<?>... proxyClasses) {
        return null;
    }

    protected Method[] findImplementationMethods(Class<?>[] proxyClasses) {
        Map<MethodDescriptor, Method> descriptorMethodMap = new HashMap<>();
        Set<MethodDescriptor> finalSet = new HashSet<>();

        for(int i = 0; i < proxyClasses.length; i ++) {
            Class<?> proxyInterface = proxyClasses[i];
            Method[] methods = proxyInterface.getMethods();
            for(int j = 0; j < methods.length; j ++) {
                MethodDescriptor descriptor = new MethodDescriptor(methods[j]);
                if(Modifier.isFinal(methods[j].getModifiers())) {
                    finalSet.add(descriptor);
                } else if(!descriptorMethodMap.containsKey(descriptor)) {
                    descriptorMethodMap.put(descriptor, methods[j]);
                }
            }
        }

        Collection<Method> results = descriptorMethodMap.values();
        for(MethodDescriptor signature : finalSet) {
            results.remove(descriptorMethodMap.get(signature));
        }

        return results.toArray(new Method[results.size()]);
    }
}
