package my.netty.rpc.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import my.netty.rpc.model.MessageRequest;
import my.netty.rpc.model.MessageResponse;
import org.objenesis.strategy.StdInstantiatorStrategy;

public class KryoPoolFactory {

    private static volatile KryoPoolFactory poolFactory = null; // 这个volatile字段是必不可少的。
    // 如何在Java中使用双重检查锁实现单例
    // http://www.importnew.com/12196.html

    private KryoFactory factory = new KryoFactory() {
        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(MessageRequest.class);
            kryo.register(MessageResponse.class);
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };

    private KryoPool pool = new KryoPool.Builder(factory).build();

    private KryoPoolFactory() {
    }

    public static KryoPool getKryoPoolInstance() {
        if(poolFactory == null) {
            synchronized (KryoPoolFactory.class) {
                if(poolFactory == null) {
                    poolFactory = new KryoPoolFactory();
                }
            }
        }
        return poolFactory.getPool();
    }

    public KryoPool getPool() {
        return pool;
    }
}
