package my.netty.rpc.serialize.protostuff;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageDecoder;

public class ProtostuffDecoder extends MessageDecoder {

    public ProtostuffDecoder(MessageCodecUtil util) {
        super(util);
    }
}
