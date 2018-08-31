package my.netty.rpc.test;

import my.netty.rpc.core.ReflectionUtils;
import my.netty.rpc.services.JdbcPersonManage;
import my.netty.rpc.services.impl.JdbcPersonManageImpl;
import my.netty.rpc.services.pojo.Person;

import java.util.List;

// 这个test结合ReflectionUtils.getClassAllMethodSignature里的注释来看。
public class testReflectionUtils {

    public static void main(String[] args) {

        ReflectionUtils utils = new ReflectionUtils();
        utils.listMethod(utils.getDeclaredMethod(JdbcPersonManage.class, "save", Person.class), false);
        System.out.println(utils.getProvider().toString().trim());

        List<String> list = utils.getClassAllMethodSignature(JdbcPersonManageImpl.class);
        for(String sEh : list) {
            System.out.println(sEh);
        }
    }
}
