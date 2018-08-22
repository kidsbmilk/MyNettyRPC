package my.netty.rpc.services.impl;

import my.netty.rpc.services.PersonManage;
import my.netty.rpc.services.pojo.Person;

import java.util.concurrent.TimeUnit;

public class PersonManageImpl implements PersonManage {

    @Override
    public int save(Person p) {
        System.out.println("person data[" + p + "] has save!");
        return 0;
    }

    @Override
    public Person getPerson(Person p) {
        return p;
    }

    @Override
    public void query(Person p) {
        // your business logic code here!
        try {
            TimeUnit.SECONDS.sleep(3);
            System.out.println("person data[" + p + "] has query!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void check() {
        throw new RuntimeException("person check fail!");
    }

    @Override
    public boolean checkAge(Person p) {
        if (p.getAge() < 18) {
            throw new RuntimeException("person check age fail!");
        } else {
            System.out.println("person check age succ!");
            return true;
        }
    }
}
