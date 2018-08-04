package my.netty.rpc.services.impl;

import my.netty.rpc.services.Store;

public class StoreImpl implements Store {

    @Override
    public void save(String object) {
        System.out.println("StoreImpl ## save: [" + object + "]");
    }
}
