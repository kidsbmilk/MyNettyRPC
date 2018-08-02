package my.netty.rpc.compiler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractAccessAdaptive implements Compiler {

//    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([$_a-zA-Z][$_a-zA-Z0-9\\.]*);"); // 原作者写的。
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+(([$_a-zA-Z][$_a-zA-Z0-9]*[.])*([$_a-zA-Z][$_a-zA-Z0-9]*))\\s*;");
//    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s+"); // 原作者写的。
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([$_a-zA-Z][$_a-zA-Z0-9]*)\\s*\\{");

    // ClassLoader，Thread.currentThread().setContextClassLoader，tomcat的ClassLoader
    // https://www.cnblogs.com/549294286/p/3714692.html
    protected ClassLoader overrideThreadContextClassLoader(ClassLoader loader) {
        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
        if(loader != null && !loader.equals(threadContextClassLoader)) {
            currentThread.setContextClassLoader(loader);
            return threadContextClassLoader;
        } else {
            return null;
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoader c1 = null;
        try {
            c1 = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ignored) {
        }
        if(c1 == null) {
            c1 = AbstractAccessAdaptive.class.getClassLoader();
            if(c1 == null) {
                try {
                    c1 = ClassLoader.getSystemClassLoader();
                } catch (Throwable ignored) {
                }
            }
        }
        return c1;
    }

    // StringWriter/PrintWriter在Java异常中的作用
    // https://blog.csdn.net/ititii/article/details/80502220
    private String report(Throwable e) {
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName() + ": ");
        if(e.getMessage() != null) {
            p.print(e.getMessage() + "\n");
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    public Class<?> compile(String code, ClassLoader classLoader) {
        code = code.trim();
        Matcher matcher = PACKAGE_PATTERN.matcher(code);
        String pkg;
        if(matcher.find()) { // 见类包中的方法注释。
            pkg = matcher.group(1); // 得到完整的包名
        } else {
            pkg = "";
        }
        matcher = CLASS_PATTERN.matcher(code);
        String cls;
        if(matcher.find()) {
            cls = matcher.group(1); // 得到类名
        } else {
            throw new IllegalArgumentException("no such class name in " + code);
        }
        String className = pkg != null && pkg.length() > 0 ? pkg + "." + cls : cls;
        try {
            return Class.forName(className, true, (classLoader != null ? classLoader : getClassLoader()));
        } catch (ClassNotFoundException e) {
            if(!code.endsWith("}")) {
                throw new IllegalStateException("the java code not ends with \"}\", code: \n" + code + "\n");
            }
            try {
                return doCompile(className, code);
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable t) {
                throw new IllegalStateException("failed to compile class, cause: " + t.getMessage() + ", class: " + className + ", code: \n" + code + "\n, stack: " + report(t));
            }
        }
    }

    protected abstract Class<?> doCompile(String clsName, String javaSource) throws Throwable;
}
