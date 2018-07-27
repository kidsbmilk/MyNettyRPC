package my.netty.rpc.core;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.*;

public class ReflectionUtils {

    private static ImmutableMap.Builder<Class, Object> builder = ImmutableMap.builder();

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

    public static Object newInstance(Class type) {
        Constructor constructor = null;
        Object[] args = new Object[0];
        try {
            constructor = type.getConstructor(new Class[]{});
        } catch (NoSuchMethodException e) {
            ;
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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getDefaultVal(Class c1) {
        if(c1.isArray()) {
            return Array.newInstance(c1.getComponentType(), 0);
        } else if(c1.isPrimitive() || builder.build().containsKey(c1)) {
            return builder.build().get(c1);
        } else {
            return newInstance(c1);
        }
    }

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
}
