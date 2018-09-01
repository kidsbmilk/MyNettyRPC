package my.netty.rpc.jmx.invoke;

import com.alibaba.druid.util.Histogram;
import my.netty.rpc.core.RpcSystemConfig;

import javax.management.JMException;
import javax.management.openmbean.*;
import java.beans.ConstructorProperties;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

// 注意：在JMX中，最核心的地方在于MBean，因为这是存数据的地方，MBeanServer以及Agent只是管理和操作数据的桥梁。
// 不管JMX应用多么复杂，最终的查数据以及改变数据，都是调用MBean里的方法。
// 从这里的实现可以发现：ModuleMetricsVisitor是ModuleMetricsVisitorMXBean的存储数据的核心部分。
public class ModuleMetricsVisitor {

    private static final long DEFAULT_INVOKE_MIN_TIMESPAN = 3600 * 1000L;
    // 以下三个用于创建下面的javax.management.openmbean.CompositeType类变量。
    private static final String[] THROWABLE_ITEMNAMES = {"message", "class", "stackTrace"};
    private static final String[] THROWABLE_ITEMDESCRIPTIONS = {"message", "class", "stackTrace"};
    private static final OpenType<?>[] THROWABLE_ITEMTYPES = new OpenType<?>[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};
    /**
     * javax.management.openmbean.SimpleType类的说明翻译如下：
     * <code> SimpleType </ code>类是<i> open type </ i>类，其实例描述所有<i> open data </ i>值，这些值既不是数组，也不是{@link CompositeData CompositeData}值，
     * 也不是{@link TabularData TabularData}值。 它将所有可能的实例预定义为静态字段，并且没有公共构造函数。 给定描述其Java类名称为<i> className </ i>的值的<code> SimpleType </ code>实例，
     * 还会设置与此<code> SimpleType </ code>实例的名称和描述相对应的内部字段 到<i> className </ i>。 换句话说，它的方法<code> getClassName </ code>，
     * <code> getTypeName </ code>和<code> getDescription </ code>都返回相同的字符串值<i> className </ i>。
     *
     * 见SimpleType构造函数的实现。
     */
    private static CompositeType THROWABLE_COMPOSITE_TYPE = null;

    private String moduleName;
    private String methodName;
    // 以下四个变量通过java.util.concurrent.atomic.AtomicLongFieldUpdater的方式被使用了。
    private volatile long invokeCount = 0L;
    private volatile long invokeSuccCount = 0L;
    private volatile long invokeFailCount = 0L;
    private volatile long invokeFilterCount = 0L;
    private long invokeTimespan = 0L;
    private long invokeMinTimespan = DEFAULT_INVOKE_MIN_TIMESPAN;
    private long invokeMaxTimespan = 0L;
    private long[] invokeHistogram; // 这个用于存放druid的统计数据，见jconsole里显示的信息。
    private Exception lastStackTrace;
    private String lastStackTraceDetail;
    private long lastErrorTime;
    private int hashKey = 0;

    private Histogram histogram = new Histogram(TimeUnit.MILLISECONDS,
            new long[]{1, 10, 100, 1000, 10 * 1000, 100 * 1000, 1000 * 1000});

    /**
     * java.util.concurrent.atomic.AtomicLongFieldUpdater类注释翻译如下：
     * 基于反射的实用程序，可以对指定类的指定{@code volatile long}字段进行原子更新。 此类设计用于原子数据结构，其中同一节点的多个字段独立地受原子更新的影响。
     * 请注意，此类中{@code compareAndSet}方法的保证比其他原子类弱。 由于此类无法确保该字段的所有使用都适用于原子访问的目的，
     * 因此只能在同一更新程序上对{@code compareAndSet}和{@code set}的其他调用保证原子性。
     */
    private final AtomicLongFieldUpdater<ModuleMetricsVisitor> invokeCountUpdater = AtomicLongFieldUpdater.newUpdater(ModuleMetricsVisitor.class, "invokeCount");
    private final AtomicLongFieldUpdater<ModuleMetricsVisitor> invokeSuccCountUpdater = AtomicLongFieldUpdater.newUpdater(ModuleMetricsVisitor.class, "invokeSuccCount");
    private final AtomicLongFieldUpdater<ModuleMetricsVisitor> invokeFailCountUpdater = AtomicLongFieldUpdater.newUpdater(ModuleMetricsVisitor.class, "invokeFailCount");
    private final AtomicLongFieldUpdater<ModuleMetricsVisitor> invokeFilterCountUpdater = AtomicLongFieldUpdater.newUpdater(ModuleMetricsVisitor.class, "invokeFilterCount");

    @ConstructorProperties({"moduleName", "methodName"})
    public ModuleMetricsVisitor(String moduleName, String methodName) {
        this.moduleName = moduleName;
        this.methodName = methodName;
        clear();
    }

    public void clear() {
        lastStackTraceDetail = "";
        invokeTimespan = 0L;
        invokeMinTimespan = DEFAULT_INVOKE_MIN_TIMESPAN;
        invokeMaxTimespan = 0L;
        lastErrorTime = 0L;
        lastStackTrace = null;
        invokeCountUpdater.set(this, 0);
        invokeSuccCountUpdater.set(this, 0);
        invokeFailCountUpdater.set(this, 0);
        invokeFilterCountUpdater.set(this, 0);
        histogram.reset();
    }

    public void reset() {
        moduleName = "";
        methodName = "";
        clear();
    }

    public void setErrorLastTimeLongVal(long lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    public long getErrorLastTimeLongVal() {
        return lastErrorTime;
    }

    public String getErrorLastTime() {
        if(lastErrorTime <= 0) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(lastErrorTime));
    }

    public String getLastStackTrace() {
        if(lastStackTrace == null) {
            return null;
        }

        StringWriter buf = new StringWriter();
        lastStackTrace.printStackTrace(new PrintWriter(buf));
        return buf.toString();
    }

    public String getStackTrace(Throwable e) {
        StringWriter buf = new StringWriter();
        e.printStackTrace(new PrintWriter(buf));

        return buf.toString();
    }

    public void setLastStackTrace(Exception lastStackTrace) {
        this.lastStackTrace = lastStackTrace;
        this.lastStackTraceDetail = getLastStackTrace();
        this.lastErrorTime = System.currentTimeMillis();
    }

    public void setLastStackTraceDetail(String lastStackTraceDetail) {
        this.lastStackTraceDetail = lastStackTraceDetail;
    }

    public String getLastStackTraceDetail() {
        return lastStackTraceDetail;
    }

    public CompositeType getThrowableCompositeType() throws JMException {
        if(THROWABLE_COMPOSITE_TYPE == null) {
            THROWABLE_COMPOSITE_TYPE = new CompositeType("Throwable",
                    "Throwable",
                    THROWABLE_ITEMNAMES,
                    THROWABLE_ITEMDESCRIPTIONS,
                    THROWABLE_ITEMTYPES);
        }

        return THROWABLE_COMPOSITE_TYPE;
    }

    public CompositeData buildErrorCompositeDate(Throwable error) throws JMException {
        if(error == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>(512);

        map.put("class", error.getClass().getName());
        map.put("message", error.getMessage());
        map.put("stackTrace", getStackTrace(error));

        return new CompositeDataSupport(getThrowableCompositeType(), map); // 这个往CompositeType填充数据创建CompositeData
        // 这个链接里提到CompositeDataSupport了，作者在这里使用的原因见：ModuleMetricsHandler.connect里的分析。
        // MBean与MXBean的区别：https://blog.csdn.net/expleeve/article/details/37502501
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public long getInvokeCount() {
        return this.invokeCountUpdater.get(this);
    }

    public void setInvokeCount(long invokeCount) {
        this.invokeCountUpdater.set(this, invokeCount);
    }

    public long incrementInvokeCount() {
        return this.invokeCountUpdater.incrementAndGet(this);
    }

    public long getInvokeSuccCount() {
        return this.invokeSuccCountUpdater.get(this);
    }

    public void setInvokeSuccCount(long invokeSuccCount) {
        this.invokeSuccCountUpdater.set(this, invokeSuccCount);
    }

    public long incrementInvokeSuccCount() {
        return this.invokeSuccCountUpdater.incrementAndGet(this);
    }

    public long getInvokeFailCount() {
        return this.invokeFailCountUpdater.get(this);
    }

    public void setInvokeFailCount(long invokeFailCount) {
        this.invokeFailCountUpdater.set(this, invokeFailCount);
    }

    public long incrementInvokeFailCount() {
        return this.invokeFailCountUpdater.incrementAndGet(this);
    }

    public long getInvokeFilterCount() {
        return this.invokeFilterCountUpdater.get(this);
    }

    public void setInvokeFilterCount(long invokeFilterCount) {
        this.invokeFilterCountUpdater.set(this, invokeFilterCount);
    }

    public long incrementInvokeFilterCount() {
        return this.invokeFilterCountUpdater.incrementAndGet(this);
    }

    public void setHistogram(Histogram histogram) {
        this.histogram = histogram;
    }

    public Histogram getHistogram() {
        return histogram;
    }

    public long[] getInvokeHistogram() {
        return RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_SUPPORT ? invokeHistogram : histogram.toArray();
    }

    public void setInvokeHistogram(long[] invokeHistogram) {
        this.invokeHistogram = invokeHistogram;
    }

    public long getInvokeTimespan() {
        return invokeTimespan;
    }

    public void setInvokeTimespan(long invokeTimespan) {
        this.invokeTimespan = invokeTimespan;
    }

    public long getInvokeMinTimespan() {
        return invokeMinTimespan;
    }

    public void setInvokeMinTimespan(long invokeMinTimespan) {
        this.invokeMinTimespan = invokeMinTimespan;
    }

    public long getInvokeMaxTimespan() {
        return invokeMaxTimespan;
    }

    public void setInvokeMaxTimespan(long invokeMaxTimespan) {
        this.invokeMaxTimespan = invokeMaxTimespan;
    }

    public int getHashKey() {
        return hashKey;
    }

    public void setHashKey(int hashKey) {
        this.hashKey = hashKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return moduleName.equals(((ModuleMetricsVisitor) obj).moduleName) && methodName.equals(((ModuleMetricsVisitor) obj).methodName);
    }

    @Override
    public String toString() {
        return String.format("<<[moduleName:%s]-[methodName:%s]>> [invokeCount:%d][invokeSuccCount:%d][invokeFilterCount:%d][invokeTimespan:%d][invokeMinTimespan:%d][invokeMaxTimespan:%d][invokeFailCount:%d][lastErrorTime:%d][lastStackTraceDetail:%s]\n",
                moduleName, methodName, invokeCount, invokeSuccCount, invokeFilterCount, invokeTimespan, invokeMinTimespan, invokeMaxTimespan, invokeFailCount, lastErrorTime, lastStackTraceDetail);
    }
}
