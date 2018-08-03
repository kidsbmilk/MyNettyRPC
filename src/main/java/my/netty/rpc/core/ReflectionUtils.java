package my.netty.rpc.core;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.*;

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
            constructor = constructors[0];
            Class[] params = constructor.getParameterTypes();
            args = new Object[params.length];
            for(int i = 0; i < params.length; i ++) {
                args[i] = getDefaultVal(params[i]);
            }
        }

        try {
            return constructor.newInstance(args);
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

    private String modifiers(int m) {
        return m != 0 ? Modifier.toString(m) + " " : "";
    }

    private String getType(Class<?> t) {
        StringBuilder brackets = new StringBuilder();
        while(t.isArray()) {
            brackets.append("[]");
            t = t.getComponentType();
        }
        return t.getName() + brackets;
    }

    private void listTypes(Class<?>[] types) {
        for(int i = 0; i < types.length; i ++) {
            if(i > 0) {
                provider.append(", ");
            }
            provider.append(getType(types[i]));
        }
    }

    private void listField(Field f, boolean html) {
        provider.append(html ? "&nbsp&nbsp&nbsp&nbsp" : "    ")
                .append(modifiers(f.getModifiers()))
                .append(getType(f.getType()))
                .append(" ")
                .append(f.getName())
                .append(html ? ";<br>" : ";\n");
    }

    private void listMethod(Executable member, boolean html) {
        provider.append(html ? "<br>&nbsp&nbsp&nbsp&nbsp" : "\n    ")
                .append(modifiers(member.getModifiers()));
        if(member instanceof Method) {
            provider.append(getType(((Method) member).getReturnType()))
                    .append(" ");
        }
        provider.append(member.getName())
                .append("(");
        listTypes(member.getParameterTypes());
        provider.append(")");
        Class<?>[] exceptions = member.getExceptionTypes();
        if(exceptions.length > 0) {
            provider.append(" throws ");
        }
        listTypes(exceptions);
        provider.append(";");
    }

    public void listRpcProviderDetail(Class<?> c, boolean html) {
        if(c.isInterface()) {
            provider.append(modifiers(c.getModifiers())) // 得到接口的修饰符，在这个导出服务里，只有接口，是面向接口编程。
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
}
