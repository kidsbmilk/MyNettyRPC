package my.netty.rpc.serialize.support.kryo;

import my.netty.rpc.serialize.support.MessageCodecUtil;
import my.netty.rpc.serialize.support.MessageEncoder;

public class KryoEncoder extends MessageEncoder {

    public KryoEncoder(MessageCodecUtil util) {
        super(util);
    }
}
