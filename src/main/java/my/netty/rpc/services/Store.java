package my.netty.rpc.services;

public interface Store {

    void save(String object);

    void save(int x);
}
