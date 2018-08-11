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

        config.setMaxTotal(maxTotal);
        config.setMinIdle(minIdle);
        config.setMaxWaitMillis(maxWaitMillis);
        config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);

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
