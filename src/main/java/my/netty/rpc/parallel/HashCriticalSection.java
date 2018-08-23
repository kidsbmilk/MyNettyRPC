package my.netty.rpc.parallel;

import my.netty.rpc.core.RpcSystemConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class HashCriticalSection {

    private static Integer partition = RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_NUMS;
    private final Map<Integer, Semaphore> criticalSectionMap = new ConcurrentHashMap<>();
    public final static long BASIC = 0xcbf29ce484222325L;
    public final static long PRIME = 0x100000001b3L;

    public HashCriticalSection() {
        boolean fair = RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_LOCK_FAIR == 1;
        init(null, fair);
    }

    public HashCriticalSection(Integer counts, boolean fair) {
        init(counts, fair);
    }

    public static int hash(String key) {
        return Math.abs((int) (fnv1a64(key) % partition));
    }

    public static long fnv1a64(String key) {
        long hashCode = BASIC;
        for(int i = 0; i < key.length(); i ++) {
            char ch = key.charAt(i);

            if(ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch + 32);
            }

            hashCode ^= ch;
            hashCode *= PRIME;
        }

        return hashCode;
    }

    private void init(Integer counts, boolean fair) {
        if(counts != null) {
            partition = counts;
        }
        for(int i = 0; i < partition; i ++) {
            criticalSectionMap.put(i, new Semaphore(1, fair));
        }
    }

    public void enter(String key) {
        int hashKey = hash(key);
        Semaphore semaphore = criticalSectionMap.get(hashKey);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void exit(String key) {
        int hashKey = hash(key);
        Semaphore semaphore = criticalSectionMap.get(hashKey);
        semaphore.release();
    }

    public void enter(int hashKey) {
        Semaphore semaphore = criticalSectionMap.get(hashKey);
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void exit(int hashKey) {
        Semaphore semaphore = criticalSectionMap.get(hashKey);
        semaphore.release();
    }
}
