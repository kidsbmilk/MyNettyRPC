package my.netty.rpc.services.impl;

import my.netty.rpc.services.AddCalculate;

public class AddCalculateImpl implements AddCalculate {

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
