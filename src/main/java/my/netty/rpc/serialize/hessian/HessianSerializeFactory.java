package my.netty.rpc.serialize.hessian;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class HessianSerializeFactory extends BasePooledObjectFactory<HessianSerialize> {

    @Override
    public HessianSerialize create() throws Exception {
        return createHessian();
    }

    @Override
    public PooledObject<HessianSerialize> wrap(HessianSerialize hessian) {
        return new DefaultPooledObject<HessianSerialize>(hessian);
    }

    private HessianSerialize createHessian() {
        return new HessianSerialize();
    }
}
