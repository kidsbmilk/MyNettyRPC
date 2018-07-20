package my.netty.rpc.serialize.kryo;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageDecoder;

public class KryoDecoder extends MessageDecoder {

    public KryoDecoder(MessageCodecUtil util) {
        super(util);
    }
}
