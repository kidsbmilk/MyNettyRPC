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

public class ByteCodeAbstractClassTransformer extends AbstractClassTransformer implements Opcodes {

    private static final AtomicLong CLASS_NUMBER = new AtomicLong(0);
    private static final String CLASSNAME_PREFIX = "ASMPROXY_";
    private static final String HANDLER_NAME = "__handler";
    private static final Type INVOKER_TYPE = Type.getType(ObjectInvoker.class); // 注意这个类型

    @Override
    public Class<?> transform(ClassLoader classLoader, Class<?>... proxyClasses) {
        Class<?> superclass = ReflectionUtils.getParentClass(proxyClasses);
        String proxyName = CLASSNAME_PREFIX + CLASS_NUMBER.incrementAndGet();
        Method[] implementationMethods = super.findImplementationMethods(proxyClasses); // 这里调用父类里的方法
        Class<?>[] interfaces = ReflectionUtils.filterInterfaces(proxyClasses);
        String classFileName = proxyName.replace('.', '/');
        // 上面这里是收集proxyClasses的基本信息，如父类、代理名称、要实现的方法、实现的接口、类存储路径。

        try {
            byte[] proxyBytes = generate(superclass, classFileName, implementationMethods, interfaces);
            return loadClass(classLoader, proxyName, proxyBytes); // 加载上面生成的类，并返回
        } catch (final Exception e) {
            throw new CreateProxyException(e);
        }
    }

    private byte[] generate(Class<?> classToProxy, String proxyName, Method[] methods, Class<?>... interfaces) throws CreateProxyException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); // 注意这个ClassWriter.COMPUTE_MAXS
        Type proxyType = Type.getObjectType(proxyName);
        String[] interfaceNames = new String[interfaces.length];
        for(int i = 0; i < interfaces.length; i ++) {
            interfaceNames[i] = Type.getType(interfaces[i]).getInternalName();
        }
        Type superType = Type.getType(classToProxy);
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, proxyType.getInternalName(), null, superType.getInternalName(), interfaceNames);
        cw.visitField(ACC_FINAL + ACC_PRIVATE, HANDLER_NAME, INVOKER_TYPE.getDescriptor(), null, null).visitEnd(); // 注意这个INVOKER_TYPE.getDescriptor()
        // 这个HANDLER_NAME在子类的构造函数中初始化，见initialize的说明
        initialize(cw, proxyType, superType); // 创建带有一个参数的构造函数
        for(final Method method : methods) {
            transformMethod(cw, method, proxyType, HANDLER_NAME); // 这个HANDLER_NAME当作私有成员变量来用的。
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
        // 注意上面的INVOKER_TYPE

        adapter.visitCode();
        adapter.loadThis(); // 把this放栈上，用于下面调用父类的构造函数
        adapter.invokeConstructor(superType, org.objectweb.asm.commons.Method.getMethod("void <init> ()"));
        adapter.loadThis(); // 把this放栈上，用于下面调用子类的构造函数对成员变量初始化，表示下面这个变量所属的类实例对象
        adapter.loadArg(0); // 把子类型构造函数中的参数（此构造函数只有一个参数，见adapter创建时的参数说明）加载进来
        adapter.putField(proxyType, HANDLER_NAME, INVOKER_TYPE); // 将构造函数中的参数赋值给子类中的变量
        /**
         * 我在网上找了很多资料，都没有找到上面三句是什么意思，然后自己写了个继承关系，然后编译，然后用javap -verbose [类名] > [文本]，
         * 最后观察文本得出的上面的结论。
         * 其实asm的手册上的解释，但是很少有人去看整个手册。
         */
        adapter.returnValue();
        adapter.endMethod();
        adapter.visitEnd();
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

    // java虚拟机指令dup的理解: http://www.cnblogs.com/CLAYJJ/archive/2017/10/20/7698035.html
    // Java栈和局部变量操作（二）: https://www.cnblogs.com/chenqiangjsj/archive/2011/04/03/2004231.html

    private void transformMethod(ClassWriter cw, Method method, Type proxyType, String handlerName) throws CreateProxyException {
        int accessType = (ACC_PUBLIC | ACC_PROTECTED) & method.getModifiers();
        org.objectweb.asm.commons.Method m = org.objectweb.asm.commons.Method.getMethod(method);
        GeneratorAdapter adapter = new GeneratorAdapter(accessType, m, null, getTypes(method.getExceptionTypes()), cw);

        adapter.visitCode();

        // 方法签名入栈
        adapter.push(Type.getType(method.getDeclaringClass())); // 将声明此方法的接口或者类入栈，这个信息是invokeVirtual要用的
        adapter.push(method.getName()); // 将方法名入栈，这个信息是invokeVirtual调用的getDeclaredMethod的第一个参数

        adapter.push(Type.getArgumentTypes(method).length); // 将方法的参数总个数入栈，这个总个数也就是数组长度，这个信息在以数据进行填充时会用到，
        // 而且在invokeVirtual调用的getDeclaredMethod的第二个参数中也会用到

        // 创建参数数组
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
        // 上面的调用会有返回Method的，此时栈顶为：...,Method（往右为栈顶的方向）
        // getDeclaredMethod(String, Class[])是根据指定的参数列表，返回相应的方法。

        /**
         * 上面的注释，可以自己写一个类继承关系，然后用javap -verbose [类名] > [文本]，观察一下结果。
         */

        adapter.loadThis(); // 这个this用于下面的加载handlerName的指令中
        adapter.getField(proxyType, handlerName, INVOKER_TYPE); // 将handlerName放栈上
        // 此时栈的内容：..., Method, this.handlerName（往右为栈顶的方向）

        // 偏移堆栈指针
        adapter.swap(); // 将栈顶两个非long或者double类型的数值交换
        // 此时栈的内容：..., this.handlerName, Method（往右为栈顶的方向）
        adapter.loadThis();
        // 此时栈的内容：..., this.handlerName, Method, this（往右为栈顶的方向）

        // 偏移堆栈指针
        adapter.swap();
        // 此时栈的内容：..., this.handlerName, this, Method（往右为栈顶的方向）

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
        // 在调用invokeInterface之前，栈的情况为：this.handlerName, this, Method, objectType（数组）（往右为栈顶的方向）
        // 在调用invokeInterface时，是指，调用this.handlerName的invoke方法，栈顶的3个值为invoke的参数。
        // 这个是调用InvocationHandler接口的invoke方法，用的是java的反射来创建对象，并返回这个对象-----这句话的理解是错的
        // 正确的理解：要结合这里的接口来看，调用的是ObjectInvoker.invoke方法。

        // 从方法的返回值拆箱
        adapter.unbox(Type.getReturnType(method)); // 将上面的invokeInterface返回的Object拆箱
        adapter.returnValue(); // 此时栈中有拆箱后的Object，返回给调用方了。
        adapter.endMethod();
        adapter.visitEnd();
    }

    // 验证下上面的注释是否正确，重排一个代码，实验成功了，说明我上面的理解是正确的。
    private void transformMethod2(ClassWriter cw, Method method, Type proxyType, String handlerName) throws CreateProxyException {
        int access = (ACC_PUBLIC | ACC_PROTECTED) & method.getModifiers();
        org.objectweb.asm.commons.Method m = org.objectweb.asm.commons.Method.getMethod(method);
        GeneratorAdapter adapter = new GeneratorAdapter(access, m, null, getTypes(method.getExceptionTypes()), cw);

        adapter.visitCode();

        adapter.loadThis();
        adapter.getField(proxyType, handlerName, INVOKER_TYPE);
        adapter.loadThis();
        // 上面两个参数是最后面的invokeInterface要用的。

        adapter.push(Type.getType(method.getDeclaringClass()));
        adapter.push(method.getName());

        adapter.push(Type.getArgumentTypes(method).length);

        Type classType = Type.getType(Class.class);
        adapter.newArray(classType);

        for(int i = 0; i < Type.getArgumentTypes(method).length; i ++) {
            adapter.dup();

            adapter.push(i);
            adapter.push(Type.getArgumentTypes(method)[i]);
            adapter.arrayStore(classType);
        }
        adapter.invokeVirtual(classType,
                org.objectweb.asm.commons.Method.getMethod("java.lang.reflect.Method getDeclaredMethod(String, Class[])"));
        adapter.push(Type.getArgumentTypes(method).length);
        Type objectType = Type.getType(Object.class);
        adapter.newArray(objectType);

        for(int i = 0; i < Type.getArgumentTypes(method).length; i ++) {
            adapter.dup();
            adapter.push(i);
            adapter.loadArg(i);
            adapter.valueOf(Type.getArgumentTypes(method)[i]);
            adapter.arrayStore(objectType);
        }
        adapter.invokeInterface(INVOKER_TYPE,
                org.objectweb.asm.commons.Method.getMethod("Object invoke(Object, java.lang.reflect.Method, Object[])"));
        adapter.unbox(Type.getReturnType(method));
        adapter.returnValue();
        adapter.endMethod();
        adapter.visitEnd();
    }

    // FIXME:
    // 这里可以考虑引入独立的类加载器，对于织入增强的字节码重新热加载到虚拟机。
    // 先这么写，后续再考虑优化。
    private Class<?> loadClass(ClassLoader loader, String className, byte[] b) {
        try {
            Method method = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            // 通过反射得到ClassLoader.defineClass方法。

            boolean accessible = method.isAccessible();
            if(!accessible) {
                method.setAccessible(true);
            }
            try {
                return (Class<?>) method.invoke(loader, className, b, Integer.valueOf(0), Integer.valueOf(b.length)); // 这里是加载类的地方
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
