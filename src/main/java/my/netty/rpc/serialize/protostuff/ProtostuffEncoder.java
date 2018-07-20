package my.netty.rpc.serialize.protostuff;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageEncoder;

public class ProtostuffEncoder extends MessageEncoder {

    public ProtostuffEncoder(MessageCodecUtil util) {
        super(util);
    }
}
