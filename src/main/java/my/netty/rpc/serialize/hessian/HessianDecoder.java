package my.netty.rpc.serialize.hessian;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageDecoder;

public class HessianDecoder extends MessageDecoder {

    public HessianDecoder(MessageCodecUtil util) {
        super(util);
    }
}
