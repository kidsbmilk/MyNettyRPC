package my.netty.rpc.compiler;

import javax.tools.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Locale;

public class NativeCompiler implements Closeable {

    private final File tempFolder;
    private final URLClassLoader classLoader;
    // Java URLClassLoader 和 ClassLoader类加载器：https://www.cnblogs.com/rogge7/p/7766522.html

    NativeCompiler(File tempFolder) {
        this.tempFolder = tempFolder;
        this.classLoader = createClassLoader(tempFolder);
    }

    private static URLClassLoader createClassLoader(File tempFolder) {
        try {
            URL[] urls = {tempFolder.toURI().toURL()};
            return new URLClassLoader(urls);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public Class<?> compile(String className, String code) {
        try {
            JavaFileObject sourceFile = new StringJavaFileObject(className, code);
            compileClass(sourceFile);
            return classLoader.loadClass(className);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void compileClass(JavaFileObject sourceFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>(); // DiagnosticCollector实现了DiagnosticListener接口。
        // DiagnosticListener - 诊断信息监听器, 编译过程触发.生成编译task(JavaCompiler#getTask())或获取FileManager(JavaCompiler#getStandardFileManager())时需要传递DiagnosticListener以便收集诊断信息。
        // Java动态编译那些事: https://www.jianshu.com/p/44395ef6406f
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, Locale.ROOT, null)) { // try-with-resources是jdk1.7以后才有的语法特性。
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(tempFolder));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, collector, null, null, Collections.singletonList(sourceFile));
            task.call();
        }
    }

    @Override
    public void close() {
        try {
            classLoader.close(); // 见包中此方法的注释。
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }
}
