package my.netty.rpc.services.impl;

import my.netty.rpc.services.Store;

public class StoreImpl implements Store {

    @Override
    public void save(String object) {
        System.out.println("StoreImpl ## save string: [" + object + "]");
    }

    @Override
    public void save(int x) {
        System.out.println("StoreImpl ## save int: [" + x + "]"); // 字符串和数字直接相加，是把数字当成了字符串，这是JAVA的装箱机制，最终相当于字符串的连接
    }
}
