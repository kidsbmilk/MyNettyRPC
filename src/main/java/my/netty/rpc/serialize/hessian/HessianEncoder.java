package my.netty.rpc.serialize.hessian;

import my.netty.rpc.serialize.MessageCodecUtil;
import my.netty.rpc.serialize.MessageEncoder;

public class HessianEncoder extends MessageEncoder {

    public HessianEncoder(MessageCodecUtil util) {
        super(util);
    }
}
