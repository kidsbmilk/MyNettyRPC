package my.netty.rpc.netty;

import my.netty.rpc.jmx.invoke.ModuleMetricsHandler;

public class GetVisitorScheduledTask implements Runnable {

    @Override
    public void run() {
        System.out.println("---------------------- get visitor ----------------------");
        ModuleMetricsHandler.getInstance().getModuleMetricsVisitorList();
//        System.out.println(ModuleMetricsHandler.getInstance().getModuleMetricsVisitorList().size());
    }

}
