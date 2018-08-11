package my.netty.rpc.services;

import my.netty.rpc.services.pojo.Person;

import java.util.List;

public interface JdbcPersonManage {

    int save(Person p);

    void query(Person p);

    List<Person> query();
}
