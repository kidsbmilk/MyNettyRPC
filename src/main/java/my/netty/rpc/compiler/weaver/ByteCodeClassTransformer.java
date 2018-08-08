package my.netty.rpc.compiler.weaver;

import my.netty.rpc.compiler.invoke.ObjectInvoker;
import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.exception.CreateProxyException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

public class ByteCodeClassTransformer extends ClassTransformer implements Opcodes {

    private static final AtomicLong CLASS_NUMBER = new AtomicLong(0);
    private static final String CLASSNAME_PREFIX = "ASMPROXY_";
    private static final String HANDLER_NAME = "__handler";
    private static final Type INVOKER_TYPE = Type.getType(ObjectInvoker.class);

    @Override
    public Class<?> transform(ClassLoader classLoader, Class<?>... proxyClasses) {
        Class<?> superclass = ReflectionUtils.getParentClass(proxyClasses);
        String proxyName = CLASSNAME_PREFIX + CLASS_NUMBER.incrementAndGet();
        Method[] implementationMethods = super.findImplementationMethods(proxyClasses);
        Class<?>[] interfaces = ReflectionUtils.filterInterfaces(proxyClasses);
        String classFileName = proxyName.replace('.', '/');

        try {
            byte[] proxyBytes = generate(superclass, classFileName, implementationMethods, interfaces);
            return loadClass(classLoader, proxyName, proxyBytes);
        } catch (final Exception e) {
            throw new CreateProxyException(e);
        }
    }

    private byte[] generate(Class<?> classToProxy, String proxyName, Method[] methods, Class<?>... interfaces) throws CreateProxyException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        Type proxyType = Type.getObjectType(proxyName);
        String[] interfaceNames = new String[interfaces.length];
        for(int i = 0; i < interfaces.length; i ++) {
            interfaceNames[i] = Type.getType(interfaces[i]).getInternalName();
        }
        Type superType = Type.getType(classToProxy);
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, proxyType.getInternalName(), null, superType.getInternalName(), interfaceNames);
        cw.visitField(ACC_FINAL + ACC_PRIVATE, HANDLER_NAME, INVOKER_TYPE.getDescriptor(), null, null).visitEnd();
        // 这个HANDLER_NAME在子类的构造函数中初始化，见initialize的说明
        initialize(cw, proxyType, superType);
        for(final Method method : methods) {
            transformMethod(cw, method, proxyType, HANDLER_NAME);
        }
        return cw.toByteArray();
    }

    // FIXME:
    // 字节码中的init方法必须先行初始化。
    // clinit就暂时不考虑了

    // 深入理解jvm--Java中init和clinit区别完全解析 : https://blog.csdn.net/u013309870/article/details/72975536

    // java字节码中的aload_0 : https://blog.csdn.net/DViewer/article/details/51138148
    // 在非静态方法中，aload_0 表示对this的操作，在static 方法中，aload_0表示对方法的第一参数的操作。

    // TODO:
    // GeneratorAdapter虽然方便，但是速度太慢了，改为asm核心包中的方法。

    // ASM（字节码处理工具）: https://blog.csdn.net/teaandnoodle/article/details/52331403

    private void initialize(ClassWriter cw, Type proxyType, Type superType) {
        GeneratorAdapter adapter = new GeneratorAdapter(ACC_PUBLIC, new org.objectweb.asm.commons.Method(
                "<init>", Type.VOID_TYPE, new Type[]{INVOKER_TYPE}), null, null, cw); // 从这个可以看到，子类的构造函数有一个参数
        adapter.loadThis(); // 把this放栈上，用于下面调用父类的构造函数
        adapter.invokeConstructor(superType, org.objectweb.asm.commons.Method.getMethod("void <init> ()"));
        adapter.loadThis(); // 把this放栈上，用于下面调用子类的构造函数对成员变量初始化，表示下面这个变量所属的类实例对象
        adapter.loadArg(0); // 把子类型构造函数中的参数（此构造函数只有一个参数，见adapter创建时的参数说明）加载进来
        adapter.putField(proxyType, HANDLER_NAME, INVOKER_TYPE); // 将构造函数中的参数赋值给子类中的变量
        /**
         * 我在网上找了很多资料，都没有找到上面三句是什么意思，然后自己写了个继承关系，然后编译，然后用javap -verbose [类名] > [文本]，
         * 最后观察文本得出的上面的结论。
         */
        adapter.returnValue();
        adapter.endMethod();
    }

    private Type[] getTypes(Class<?>... src) {
        Type[] result = new Type[src.length];
        for(int i = 0; i < result.length; i ++) {
            result[i] = Type.getType(src[i]);
        }

        return result;
    }

    // FIXME:
    // 本来想用cglib来实现对字节码的控制，但是考虑到性能问题，决定采用偏向底层的ASM对JVM的字节码进行渲染织入增强。
    // 其中获取方法签名通过反射方式取得，虽然性能上可能有所损失，但是编码方式比较简洁，不会出现大量的ASM堆栈操作的API序列。

    private void transformMethod(ClassWriter cw, Method method, Type proxyType, String handlerName) throws CreateProxyException {
        int access = (ACC_PUBLIC | ACC_PROTECTED) & method.getModifiers();
        org.objectweb.asm.commons.Method m = org.objectweb.asm.commons.Method.getMethod(method);
        GeneratorAdapter adapter = new GeneratorAdapter(access, m, null, getTypes(method.getExceptionTypes()), cw);

        // 方法签名入栈
        adapter.push(Type.getType(method.getDeclaringClass()));
        adapter.push(method.getName());
        adapter.push(Type.getArgumentTypes(method).length);

        // 创建Class对象
        Type classType = Type.getType(Class.class);
        adapter.newArray(classType); // 用classType这个Type来表示创建好的数组，即便是两个类型相同的数组，只要Type名不同，就可以表示两个不同的数组

        // 获取方法参数列表
        for(int i = 0; i < Type.getArgumentTypes(method).length; i ++) {
            // 从方法堆栈顶复制一份参数类型
            adapter.dup(); // 将当前的栈顶元素复制一份，并压入栈中，当前栈顶的元素是数组长度

            // 把参数索引入栈
            adapter.push(i); // 数组元素中的索引
            adapter.push(Type.getArgumentTypes(method)[i]); // 数组元素中的索引对应的数组中的元素
            adapter.arrayStore(classType); // 将此classType表示的数组中的对应索引处的值设置为给定的元素值
        }

        // 调用getDeclaredMethod方法
        adapter.invokeVirtual(classType,
                org.objectweb.asm.commons.Method.getMethod("java.lang.reflect.Method getDeclaredMethod(String, Class[])")); // 注意字符串最后面的')'

        adapter.loadThis();
        adapter.getField(proxyType, handlerName, INVOKER_TYPE);

        // 偏移堆栈指针
        adapter.swap(); // 将栈顶两个非long或者double类型的数值交换
        adapter.loadThis();

        // 偏移堆栈指针
        adapter.swap();

        // 获取方法的参数取值列表
        adapter.push(Type.getArgumentTypes(method).length);
        Type objectType = Type.getType(Object.class);
        adapter.newArray(objectType);

        for(int i = 0; i < Type.getArgumentTypes(method).length; i ++) {
            // 从方法堆栈顶复制一份参数类型
            adapter.dup(); // 前栈顶的元素是数组长度
            adapter.push(i);
            adapter.loadArg(i);
            adapter.valueOf(Type.getArgumentTypes(method)[i]);
            adapter.arrayStore(objectType);
        }

        // 调用方法
        adapter.invokeInterface(INVOKER_TYPE,
                org.objectweb.asm.commons.Method.getMethod("Object invoke(Object, java.lang.reflect.Method, Object[])")); // 注意字符串最后面的')'

        // 从方法的返回值拆箱
        adapter.unbox(Type.getReturnType(method));
        adapter.returnValue();
        adapter.endMethod();
    }

    // FIXME:
    // 这里可以考虑引入独立的类加载器，对于织入增强的字节码重新热加载到虚拟机。
    // 先这么写，后续再考虑优化。
    private Class<?> loadClass(ClassLoader loader, String className, byte[] b) {
        try {
            Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);

            boolean accessible = method.isAccessible();
            if(!accessible) {
                method.setAccessible(true);
            }
            try {
                return (Class<?>) method.invoke(loader, className, b, Integer.valueOf(0), Integer.valueOf(b.length));
            } finally {
                if(!accessible) {
                    method.setAccessible(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
