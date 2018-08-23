package my.netty.rpc.services.impl;

import my.netty.rpc.services.CostTimeCalculate;
import my.netty.rpc.services.pojo.CostTime;

public class CostTimeCalculateImpl implements CostTimeCalculate {

    @Override
    public CostTime calculate() {
        CostTime elapse = new CostTime();
        try {
            long start = 0, end = 0;
            start = System.currentTimeMillis();
            //模拟耗时操作
            Thread.sleep(5 * 1000L);
            end = System.currentTimeMillis();

            long interval = end - start;
            elapse.setElapse(interval);
            elapse.setDetail("cost time operate success.");
            System.out.println("calculate time: " + interval);
            return elapse;
        } catch (InterruptedException e) {
            e.printStackTrace();
            elapse.setDetail("cost time operate fail!");
            return elapse;
        }
    }

    @Override
    public CostTime busy() {
        CostTime elapse = new CostTime();
        try {
            long start = 0, end = 0;
            start = System.currentTimeMillis();
            // 模拟耗时操作，超过nettyrpc.default.asyncmsg.timeout定义的上限
            Thread.sleep(35 * 1000L);
            end = System.currentTimeMillis();

            long interval = end - start;
            elapse.setElapse(interval);
            elapse.setDetail("I'm zz, I'm busy now!");
            System.out.println("calculate time: " + interval);
            return elapse;
        } catch (InterruptedException e) {
            e.printStackTrace();
            elapse.setDetail("I'm zz, I'm handle error now");
            return elapse;
        }
    }
}
