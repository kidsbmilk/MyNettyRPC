package my.netty.rpc.serialize.kryo;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageEncoder;

public class KryoEncoder extends MessageEncoder {

    public KryoEncoder(MessageCodecUtil util) {
        super(util);
    }
}
