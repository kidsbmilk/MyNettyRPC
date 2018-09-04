package my.netty.rpc.netty;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.filter.ServiceFilterBinder;
import my.netty.rpc.jmx.invoke.HashModuleMetricsVisitor;
import my.netty.rpc.jmx.invoke.ModuleMetricsVisitor;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import my.netty.rpc.parallel.HashCriticalSection;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;

import javax.management.JMException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class HashMessageRecvInitializeTask extends AbstractMessageRecvInitializeTask {

    private int hashKey = 0;
    private static HashCriticalSection criticalSection = new HashCriticalSection();
    private AtomicReference<ModuleMetricsVisitor> visitor = new AtomicReference<>();

    public HashMessageRecvInitializeTask(MessageRequest request, MessageResponse response, Map<String, Object> handlerMap) {
        super(request, response, handlerMap);
        hashKey = HashCriticalSection.hash(request.getMessageId());
    }

    // 作者又是刻意在学习、练习，在MessageRecvInitializeTask中，作者使用了watcher那一套通信机制来实现数据交换，
    // 到这一版，已经抛弃那一套了。
    // 可以对比一下这里的父类抽象方法的实现与MessageRecvInitializeTask中父类抽象方法的实现。
    @Override
    protected void injectInvoke() {
        Class cls = handlerMap.get(request.getClassName()).getClass();
        boolean binder = ServiceFilterBinder.class.isAssignableFrom(cls);
        if(binder) {
            cls = ((ServiceFilterBinder) handlerMap.get(request.getClassName())).getObject().getClass();
        }

        ReflectionUtils utils = new ReflectionUtils();

        try {
            Method method = ReflectionUtils.getDeclaredMethod(cls, request.getMethodName(), request.getTypeParameters());
            utils.listMethod(method, false);
            String signatureMethod = utils.getProvider().toString().trim(); // 注意这个trim()，以及其他说明见ReflectionUtils.getClassAllMethodSignature里的注释。
            LOGGER.info("signatureMethod: {}", signatureMethod);

            int index = getHashVisitorListIndex(signatureMethod);
            LOGGER.info("index: {}", index);
            List<ModuleMetricsVisitor> metricsVisitors = HashModuleMetricsVisitor.getInstance().getHashVisitorLists().get(index);
            /**
             * AbstractModuleMetricsHandler.getVisitor要通过并发手段来实现动态安全增加（见AbstractMessageRecvInitializeTask.call里的注释），
             * 而这里Hash版本通过事先创建所有visitor，之后只会涉及到多线程并发的读，消除了并发修改的问题。
             */
            LOGGER.info("hashKey: {}", hashKey);
            visitor.set(metricsVisitors.get(hashKey));
            /**
             * 对于metricsVisitor，是同一个类同一种方法的RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_NUMS个visitor，
             * 只是metricsVisitor.hashCode不同，这个metricsVisitor.hashCode并没有使用到，在排查线程问题时可以用到。
             *
             * 而这个visitor是从那么多个同类的visitor里选一个，根据request.getMessageId()的hash值来选。
             *
             * request.getMessageId()的hash值也决定了加锁修改visitor值时
             * （为什么还要加锁：因为不可能每个操作都创建一个新的visitor来统计信息，所以在大量请求的情况下，
             * 必然存在两种同样操作的任务使用相同的visitor的可能，所以就需要使用并发手段来做到安全修改信息。）
             * 选择的是哪个Semaphore（见HashCriticalSection.enter的实现）。
             *
             * 这个解决了AbstractMessageRecvInitializeTask.call的注释里的第二个并发问题。
             *
             * 注意一点：如果同一种操作的两个请求，映射到同一相visitor上了，那么两个请求需也映射到同一把锁上，这一点非常重要，作者也是这样实现的。
             * 那么，这里的实现也有一个小问题，不同的操作可能映射到同一把锁上，还可以再改一下，更加提高效率，比如：每个visitor对应一把锁。TODO-THIS.
             */
            incrementInvoke(visitor.get());
        } finally {
            utils.clearProvider();
        }
    }

    @Override
    protected void injectSuccInvoke(long invokeTimespan) {
        incrementInvokeSucc(visitor.get(), invokeTimespan);
    }

    @Override
    protected void injectFailInvoke(Throwable error) {
        incrementInvokeFail(visitor.get(), error);
    }

    @Override
    protected void injectFilterInvoke() {
        incrementInvokeFilter(visitor.get());
    }

    @Override
    protected void acquire() {
        criticalSection.enter(hashKey);
    }

    @Override
    protected void release() {
        criticalSection.exit(hashKey);
    }

    private int getHashVisitorListIndex(String signatureMethod) {
        int index = 0;
        int size = HashModuleMetricsVisitor.getInstance().getHashModuleMetricsVisitorListsSize();
        for(index = 0; index < size; index ++) {
            Iterator iterator = new FilterIterator(HashModuleMetricsVisitor.getInstance().getHashVisitorLists().get(index).iterator(), new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    String statModuleName = ((ModuleMetricsVisitor) object).getModuleName();
                    String statMethodName = ((ModuleMetricsVisitor) object).getMethodName();
                    return statModuleName.compareTo(request.getClassName()) == 0 && statMethodName.compareTo(signatureMethod) == 0;
                }
            });

            if(iterator.hasNext()) {
                break; // break标记是用来标记跳出哪个循环的，并不是说跳到哪里去执行，之前的理解想当然了。
            }
        }
        return index;
    }

    private void incrementInvoke(ModuleMetricsVisitor visitor) {
        visitor.setHashKey(hashKey);
        visitor.incrementInvokeCount();
    }

    private void incrementInvokeSucc(ModuleMetricsVisitor visitor, long invokeTimespan) {
        visitor.incrementInvokeSuccCount();
        visitor.getHistogram().record(invokeTimespan); // 这个invokeTimespan的由来，见MethodInvoker.invoke里的注释。
        visitor.setInvokeTimespan(invokeTimespan);

        if(invokeTimespan < visitor.getInvokeMinTimespan()) {
            visitor.setInvokeMinTimespan(invokeTimespan);
        }

        if(invokeTimespan > visitor.getInvokeMaxTimespan()) {
            visitor.setInvokeMaxTimespan(invokeTimespan);
        }
    }

    private void incrementInvokeFail(ModuleMetricsVisitor visitor, Throwable error) {
        visitor.incrementInvokeFailCount();
        visitor.setLastStackTrace((Exception) error);
        try {
            visitor.buildErrorCompositeDate(error);
        } catch (JMException e) {
            e.printStackTrace();
        }
    }

    private void incrementInvokeFilter(ModuleMetricsVisitor visitor) {
        visitor.incrementInvokeFilterCount();
    }
}
