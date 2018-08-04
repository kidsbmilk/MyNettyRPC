package my.netty.rpc.services;

import my.netty.rpc.services.pojo.Person;

public interface PersonManage {
    int save(Person p);
    Person getPerson(Person p);

    void query(Person p);
}
