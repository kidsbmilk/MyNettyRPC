package my.netty.rpc.serialize.support.kryo;

import com.google.common.io.Closer;
import io.netty.buffer.ByteBuf;
import my.netty.rpc.serialize.support.MessageCodecUtil;

import com.esotericsoftware.kryo.pool.KryoPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KryoCodecUtil implements MessageCodecUtil {

    private KryoPool pool;
    private static Closer closer = Closer.create();

    public KryoCodecUtil(KryoPool pool) {
        this.pool = pool;
    }

    public void encode(final ByteBuf out, final Object message) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            closer.register(byteArrayOutputStream);
            KryoSerize kryoSerialization = new KryoSerialize(pool);
            kryoSerialization.serialize(byteArrayOutputStream, message);
            byte[] body = byteArrayOutputStream.toByteArray();
            int dataLength = body.length;
            out.writeInt(dataLength);
            out.writeBytes(body);
        } finally {
            closer.close();
        }
    }
}
