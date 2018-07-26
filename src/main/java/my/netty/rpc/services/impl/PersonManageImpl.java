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
}
