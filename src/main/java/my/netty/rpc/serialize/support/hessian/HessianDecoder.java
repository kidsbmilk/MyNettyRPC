package my.netty.rpc.serialize.support.hessian;

import my.netty.rpc.serialize.support.MessageCodecUtil;
import my.netty.rpc.serialize.support.MessageDecoder;

public class HessianDecoder extends MessageDecoder {

    public HessianDecoder(MessageCodecUtil util) {
        super(util);
    }
}
