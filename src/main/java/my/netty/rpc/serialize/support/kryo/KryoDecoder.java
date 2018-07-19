package my.netty.rpc.serialize.support.kryo;

import my.netty.rpc.serialize.support.MessageCodecUtil;
import my.netty.rpc.serialize.support.MessageDecoder;

public class KryoDecoder extends MessageDecoder {

    public KryoDecoder(MessageCodecUtil util) {
        super(util);
    }
}
