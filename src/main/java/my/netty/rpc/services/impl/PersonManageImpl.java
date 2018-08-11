package my.netty.rpc.services.impl;

import my.netty.rpc.services.PersonManage;
import my.netty.rpc.services.pojo.Person;

public class PersonManageImpl implements PersonManage {

    public int save(Person p) {
        System.out.println("person data[" + p + "] has save!");
        return 0;
    }

    public Person getPerson(Person p) {
        return p;
    }

    public void query(Person p) {
        // your business logic code here!
        System.out.println("person data[" + p + "] has query!");
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
