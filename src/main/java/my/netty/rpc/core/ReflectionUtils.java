package my.netty.rpc.core;

import com.google.common.collect.ImmutableMap;
import my.netty.rpc.exception.CreateProxyException;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReflectionUtils {

    private static ImmutableMap.Builder<Class, Object> builder = ImmutableMap.builder();
    private StringBuilder provider = new StringBuilder();

    public StringBuilder getProvider() {
        return provider;
    }

    public void clearProvider() {
        provider.delete(0, provider.length());
    }

    static {
        builder.put(Boolean.class, Boolean.FALSE);
        builder.put(Byte.class, Byte.valueOf((byte) 0));
        builder.put(Character.class, Character.valueOf((char) 0));
        builder.put(Short.class, Short.valueOf((short) 0));
        builder.put(Double.class, Double.valueOf(0));
        builder.put(Float.class, Float.valueOf(0));
        builder.put(Integer.class, Integer.valueOf(0));
        builder.put(Long.class, Long.valueOf(0));
        builder.put(boolean.class, Boolean.FALSE);
        builder.put(byte.class, Byte.valueOf((byte) 0));
        builder.put(char.class, Character.valueOf((char) 0));
        builder.put(short.class, Short.valueOf((short) 0));
        builder.put(double.class, Double.valueOf(0));
        builder.put(float.class, Float.valueOf(0));
        builder.put(int.class, Integer.valueOf(0));
        builder.put(long.class, Long.valueOf(0));
    }

    public static Class<?>[] filterInterfaces(Class<?>[] proxyClasses) {
        Set<Class<?>> interfaces = new HashSet<>();
        for(Class<?> proxyClass : proxyClasses) {
            if(proxyClass.isInterface()) {
                interfaces.add(proxyClass);
            }
        }

        interfaces.add(Serializable.class);
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    public static Class<?>[] filterNonInterfaces(Class<?>[] proxyClasses) {
        Set<Class<?>> superclasses = new HashSet<>();
        for(Class<?> proxyClass : proxyClasses) {
            if(!proxyClass.isInterface()) {
                superclasses.add(proxyClass);
            }
        }

        return superclasses.toArray(new Class[superclasses.size()]);
    }

    public static boolean existDefaultConstructor(Class<?> superclass) {
        final Constructor<?>[] declaredConstructors = superclass.getDeclaredConstructors(); // jdk里的方法
        for(int i = 0; i < declaredConstructors.length; i ++) {
            Constructor<?> constructor = declaredConstructors[i];
            boolean isExist = (constructor.getParameterTypes().length == 0
                    && (Modifier.isPublic(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())));
            if(isExist) {
                return true;
            }
        }

        return false;
    }

    public static Class<?> getParentClass(Class<?>[] proxyClasses) {
        final Class<?>[] parent = filterNonInterfaces(proxyClasses);
        switch (parent.length) {
            case 0 : return Object.class;
            case 1 :
                {
                    Class<?> superClass = parent[0];
                    if (Modifier.isFinal(superClass.getModifiers())) {
                        throw new CreateProxyException("proxy can't build " + superClass.getName() + ", because it is final");
                    }
                    if (!existDefaultConstructor(superClass)) { // 因为后面的反射都是通过无参的newInstance创建对象的，所以这里需要找到存在默认构造函数的父类
                        throw new CreateProxyException("proxy can't build " + superClass.getName() + ", because it has no default constructor");
                    }
                    return superClass;
                }
            default:
                {
                    StringBuilder errorMessage = new StringBuilder("proxy class can't build");
                    for (int i = 0; i < parent.length; i++) {
                        Class<?> c = parent[i];
                        errorMessage.append(c.getName());
                        if (i != parent.length - 1) {
                            errorMessage.append(", ");
                        }
                    }

                    errorMessage.append("; multiple implement not allowed");
                    throw new CreateProxyException(errorMessage.toString());
                }
        }
    }

    public static boolean isHashCodeMethod(Method method) {
        return "hashCode".equals(method.getName())
                && Integer.TYPE.equals(method.getReturnType())
                && method.getParameterTypes().length == 0;
    }

    public static boolean isEqualsMethod(Method method) {
        return "equals".equals(method.getName())
                && Boolean.TYPE.equals(method.getReturnType())
                && method.getParameterTypes().length == 1
                && Object.class.equals(method.getParameterTypes()[0]); // 注意最后这个类型
    }

    /**
     * 用Object[0]来代替null 很多时候我们需要传递参数的类型，而不是传null，所以用Object[0] : https://bbs.csdn.net/topics/390182138
     *
     * collection.toArray(new String[0])中new String[0]的作用: https://www.cnblogs.com/blog-cq/p/5680104.html
     */
    public static Object newInstance(Class type) {
        Constructor constructor = null;
        Object[] args = new Object[0];
        try {
            constructor = type.getConstructor(new Class[]{}); // 如何通过反射来创建对象？getConstructor()和getDeclaredConstructor()区别？
            // https://www.cnblogs.com/jiangyi-uestc/p/5686264.html
        } catch (NoSuchMethodException ignored) {
        }

        if(constructor == null) {
            Constructor[] constructors = type.getConstructors();
            if(constructors.length == 0) {
                return null;
            }
            constructor = constructors[0]; // 这里选择了第一个构造函数。这样会不会有总是。TODO-this。
            Class[] params = constructor.getParameterTypes();
            args = new Object[params.length];
            for(int i = 0; i < params.length; i ++) {
                args[i] = getDefaultVal(params[i]);
            }
        }

        try {
            return constructor.newInstance(args); // 使用默认的参数列表创建对象。
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getDefaultVal(Class c1) {
        if(c1.isArray()) {
            return Array.newInstance(c1.getComponentType(), 0); // Java编程：Java的反射机制中的 getComponentType() 方法
            // https://blog.csdn.net/claram/article/details/53412256
        } else if(c1.isPrimitive() || builder.build().containsKey(c1)) {
            return builder.build().get(c1);
        } else {
            return newInstance(c1); // 递归创建对象
        }
    }

    /**
     * Java类型中ParameterizedType，GenericArrayType，TypeVariabl，WildcardType详解
     * https://blog.csdn.net/sinat_29581293/article/details/52227953
     *
     * 我眼中的Java-Type体系(2)
     * https://www.jianshu.com/p/e8eeff12c306
     *
     * java Type 详解
     * https://blog.csdn.net/gdutxiaoxu/article/details/68926515
     *
     * 此函数名的意思：得到一般的类，这里的实现也是对于GenericArrayType，得到脱去其最右边的一对[]后的类型;
     * 对于ParameterizedType，得到去除其泛型参数符信息后的类型;对于非GenericArrayType、非ParameterizedType的类型，
     * 直接返回其类型。
     */
    public static Class<?> getGenericClass(ParameterizedType parameterizedType, int i) {
        Object genericClass = parameterizedType.getActualTypeArguments()[i];

        if(genericClass instanceof GenericArrayType) {
            return (Class<?>) ((GenericArrayType) genericClass).getGenericComponentType();
        } else if(genericClass instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericClass).getRawType();
        } else {
            return (Class<?>) genericClass;
        }
    }

    private String getModifiersString(int m) {
        return m != 0 ? Modifier.toString(m) + " " : "";
    }

    private String getClassType(Class<?> t) {
        StringBuilder brackets = new StringBuilder();
        while(t.isArray()) {
            brackets.append("[]");
            t = t.getComponentType();
        }
        return t.getName() + brackets;
    }

    private void listClassArrayTypes(Class<?>[] types) {
        for(int i = 0; i < types.length; i ++) {
            if(i > 0) {
                provider.append(", ");
            }
            provider.append(getClassType(types[i]));
        }
    }

    private void listField(Field f, boolean html) {
        provider.append(html ? "&nbsp&nbsp&nbsp&nbsp" : "    ")
                .append(getModifiersString(f.getModifiers()))
                .append(getClassType(f.getType()))
                .append(" ")
                .append(f.getName())
                .append(html ? ";<br>" : ";\n");
    }

    public void listMethod(Executable member, boolean html) {
        provider.append(html ? "<br>&nbsp&nbsp&nbsp&nbsp" : "\n    ")
                .append(getModifiersString(member.getModifiers() & (~Modifier.FINAL)));
        if(member instanceof Method) {
            provider.append(getClassType(((Method) member).getReturnType()))
                    .append(" ");
        }
        provider.append(member.getName())
                .append("(");
        listClassArrayTypes(member.getParameterTypes());
        provider.append(")");
        Class<?>[] exceptions = member.getExceptionTypes();
        if(exceptions.length > 0) {
            provider.append(" throws ");
        }
        listClassArrayTypes(exceptions);
        provider.append(";");
    }

    public void listRpcProviderDetail(Class<?> c, boolean html) {
        if(c.isInterface()) {
            provider.append(getModifiersString(c.getModifiers())) // 得到接口的修饰符，在这个导出服务里，只有接口，是面向接口编程。
                    .append(" ")
                    .append(c.getName()); // 接口名
            provider.append(html ? "&nbsp{<br>" : " {\n");

            boolean hasFields = false;
            Field[] fields = c.getDeclaredFields();
            if(fields.length != 0) {
                provider.append(html ? "&nbsp&nbsp&nbsp&nbsp//&nbspFields<br>" : "    // Fields\n");
                hasFields = true;
                for(Field field : fields) {
                    listField(field, html);
                }
            }

            provider.append(hasFields ? (html ? "<br>&nbsp&nbsp&nbsp&nbsp//&nbspMethods" : "\n    // Methods") : (html ? "&nbsp&nbsp&nbsp&nbsp//&nbspMethods" : "    // Methods"));
            Method[] methods = c.getDeclaredMethods();
            for(Method method : methods) {
                listMethod(method, html);
            }
            provider.append(html ? "<br>}<p>" : "\n}\n\n");
        }
    }

    public static Method getDeclaredMethod(Class<?> cls, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        Class<?> searchType = cls;
        while(searchType != null) {
            method = findDeclaredMethod(searchType, methodName, parameterTypes);
            if(method != null) {
                return method;
            }
            searchType = searchType.getSuperclass();
        }
        return method;
    }

    public static Method findDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>... parameterTypes) {
        try {
            return cls.getDeclaredMethod(methodName, parameterTypes); // 见jvm中方法的说明。
        } catch (NoSuchMethodException ignored) {
        }
        for(Method m : cls.getDeclaredMethods()) { // 见jvm中方法的说明。
            if(m.getName().equals(methodName)) {
                boolean find = true;
                Class[] paramType = m.getParameterTypes();
                if(paramType.length != parameterTypes.length) {
                    continue;
                }
                for(int i = 0; i < parameterTypes.length; i ++) {
                    if(!paramType[i].isAssignableFrom(parameterTypes[i])) {
                        find = false;
                        break;
                    }
                }
                if(find) {
                    return m;
                }
            }
        }
        return null;
    }

    // 区别get*与list*方法，list*方法用到了内部的一个变量provider，而get*方法是直接返回String。
    private String getClassArrayType(Class<?>[] types) {
        StringBuilder type = new StringBuilder();
        for(int i = 0; i < types.length; i ++) {
            if(i > 0) {
                type.append(", ");
            }
            type.append(getClassType(types[i]));
        }
        return type.toString();
    }

    public List<String> getClassAllMethodSignature(Class<?> cls) { // 使用javap -s 类名可以显示类的方法签名
        List<String> list = new ArrayList<>();
        if(cls.isInterface()) {
            Method[] methods = cls.getDeclaredMethods();
            StringBuilder signatureMethod = new StringBuilder();
            for(Method member : methods) {
                int modifiers = member.getModifiers();
                // Interface里的方法一般默认是抽象且公有的，不同的版本中，1.8会允许默认方法存在，但是不确定还是不是抽象的。
                // https://zhidao.baidu.com/question/748488169527119572.html
                // 在listMethod中去除final时，我感觉这里不需要处理final了。
                // 应该是这样的：接口中的抽象方法，在具体实现类中重写时，可以添加上final标志，我实验了下是可以的，
                // 所以listMethod中要去除可能存在的final，而这里是不用处理的。
                // final类不能有子类，final方法可以被子类继承但不能被子类改写。
                if(Modifier.isAbstract(modifiers) && Modifier.isPublic(modifiers)) {
                    signatureMethod.append(getModifiersString(Modifier.PUBLIC));
                    // 作者这里还判断了一下是否为final，其实是没有必要的，也是错误的，因为final不能与abstract一块使用，那样没有意义。
                } else {
                    signatureMethod.append(getModifiersString(modifiers)); // 这里算是修复了作者写的一个bug。
                }

                // HashMessageRecvInitializeTask里有调用listMethod，以及getHashVisitorListIndex，所以这里要把两者的实现做到一致。
                // 感觉应该是一致的，但是，我改成一致后，又出问题了，
                //  signatureMethod.append(getModifiersString(member.getModifiers())); // 为什么把上面的替换为这一句后AsyncRpcCallTest就会出错呢？
                // 我知道为什么了：因为HashMessageRecvInitializeTask里有调用listMethod时，是接口的方法的具体子类的实现，是没有abstract的，所以这里判断了一下，
                // 把public abstract的直接输出为public，这样就和HashMessageRecvInitializeTask里调用listMethod时的对上了，
                // 而如果和改成listMethod一样的话，恰恰因为多了个abstract，就对不上了。

                // 可以看出，这里是先判断了一样是否为接口，然后才输出所有方法的，也体现了面向接口编程，rpc服务对外提供的只有接口。

                // 突然发现，小细节之处，藏着大道理。

                // 可以增加log4j2日志，看看详细过程。TODO-THIS。

                signatureMethod.append(getClassType(((Method) member).getReturnType())).append(" ");

                signatureMethod.append(member.getName()).append("(");
                signatureMethod.append(getClassArrayType(member.getParameterTypes()));
                signatureMethod.append(")");
                Class<?>[] exceptions = member.getExceptionTypes();
                if(exceptions.length > 0) {
                    signatureMethod.append(" throws ");
                }
                signatureMethod.append(getClassArrayType(exceptions));
                signatureMethod.append(";");
                list.add(signatureMethod.toString());
                signatureMethod.delete(0, signatureMethod.length());
            }
        }
        return list;
    }
}
