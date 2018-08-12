package my.netty.rpc.serialize.hessian;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import static my.netty.rpc.core.RpcSystemConfig.*;

public class HessianSerializePool {

    private GenericObjectPool<HessianSerialize> hessianPool;
    private static volatile HessianSerializePool poolFactory = null; // 这个volatile字段是必不可少的。
    // 如何在Java中使用双重检查锁实现单例
    // http://www.importnew.com/12196.html

    private HessianSerializePool() {
        hessianPool = new GenericObjectPool<HessianSerialize>(new HessianSerializeFactory());
    }

    public static HessianSerializePool getHessianPoolInstance() {
        if(poolFactory == null) {
            synchronized (HessianSerializePool.class) {
                if(poolFactory == null) {
                    poolFactory = new HessianSerializePool(SERIALIZE_POOL_MAX_TOTAL, SERIALIZE_POOL_MIN_IDLE, SERIALIZE_POOL_MAX_WAIT_MILLIS, SERIALIZE_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS);
                }
            }
        }
        return poolFactory;
    }

    public HessianSerializePool(final int maxTotal, final int minIdle, final long maxWaitMillis, final long minEvictableIdleTimeMillis) {
        hessianPool = new GenericObjectPool<HessianSerialize>(new HessianSerializeFactory());

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();

        // GenericObjectPool参数解析：https://segmentfault.com/a/1190000011608913
        config.setMaxTotal(maxTotal); // 设置对象池中最大的对象数，默认为8
        config.setMinIdle(minIdle); // 设置对象池中最少空闲的对象数，默认为0
        config.setMaxWaitMillis(maxWaitMillis); // 当对象池资源耗尽时，等待时间，超出则抛出异常，默认为-1即永不超时
        config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis); // 对象空闲的最小时间，达到此值后空闲对象将可能会被移除。

        hessianPool.setConfig(config);
    }

    public HessianSerialize borrow() {
        try {
            return getHessianPool().borrowObject();
        } catch (final Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void restore(final HessianSerialize object) {
        getHessianPool().returnObject(object);
    }

    public GenericObjectPool<HessianSerialize> getHessianPool() {
        return hessianPool;
    }
}
