package my.netty.rpc.jmx.invoke;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.core.RpcSystemConfig;
import my.netty.rpc.netty.MessageRecvExecutor;

import java.util.*;

public class HashModuleMetricsVisitor {

    private List<List<ModuleMetricsVisitor>> hashVisitorLists = new ArrayList<>();

    private static final HashModuleMetricsVisitor INSTANCE = new HashModuleMetricsVisitor();

    private HashModuleMetricsVisitor() {
        init();
    }

    public static HashModuleMetricsVisitor getInstance() {
        return INSTANCE;
    }

    public int getHashModuleMetricsVisitorListSize() {
        return hashVisitorLists.size();
    }

    private void init() {
        Map<String, Object> map = MessageRecvExecutor.getInstance().getHandlerMap();
        ReflectionUtils utils = new ReflectionUtils();
        Set<String> s = map.keySet();
        Iterator<String> iterator = s.iterator();
        String key;
        while(iterator.hasNext()) {
            key = iterator.next();
            try {
                List<String> list = utils.getClassAllMethodSignature(Class.forName(key));
                for(String signature : list) {
                    List<ModuleMetricsVisitor> visitorList = new ArrayList<>(); // 这里的visitor的moduleName以及methodName都一样，只是hashKey不一样，
                    // 个数由RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_NUMS控制。
                    for(int i = 0; i < RpcSystemConfig.SYSTEM_PROPERTY_JMX_METRICS_HASH_NUMS; i ++) {
                        ModuleMetricsVisitor visitor = new ModuleMetricsVisitor(key, signature);
                        visitor.setHashKey(i);
                        visitorList.add(visitor);
                    }
                    hashVisitorLists.add(visitorList);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void signal() {
        ModuleMetricsHandler.getInstance().getLatch().countDown();
    }

    public List<List<ModuleMetricsVisitor>> getHashVisitorLists() {
        return hashVisitorLists;
    }

    public void setHashVisitorLists(List<List<ModuleMetricsVisitor>> hashVisitorLists) {
        this.hashVisitorLists = hashVisitorLists;
    }
}
