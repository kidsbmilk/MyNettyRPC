package my.netty.rpc.core;

import my.netty.rpc.netty.MessageRecvExecutor;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AbilityDetailProvider implements AbilityDetail {

    @Override
    public StringBuilder listAbilityDetail() {
        Map<String, Object> map = MessageRecvExecutor.getInstance().getHandlerMap();

        ReflectionUtils utils = new ReflectionUtils();

        Set<String> s = map.keySet();
        Iterator<String> iterator = s.iterator();
        String key;
        while(iterator.hasNext()) {
            key = iterator.next();
            try {
                utils.listRpcProviderDetail(Class.forName(key));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return utils.getProvider();
    }
}
