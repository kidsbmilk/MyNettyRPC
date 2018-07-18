package my.netty.rpc.serialize.support.hessian;

import my.netty.rpc.serialize.support.MessageCodecUtil;
import my.netty.rpc.serialize.support.MessageEncoder;

public class HessianEncoder extends MessageEncoder {

    public HessianEncoder(MessageCodecUtil util) {
        super(util);
    }
}
