package my.netty.rpc.services.impl;

import my.netty.rpc.services.MultiCalculate;

public class MultiCalculateImpl implements MultiCalculate {

    public int multi(int a, int b) {
        return a * b;
    }
}
