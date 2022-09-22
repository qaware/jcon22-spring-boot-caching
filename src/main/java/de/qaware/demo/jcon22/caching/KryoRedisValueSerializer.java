package de.qaware.demo.jcon22.caching;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.minlog.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

/**
 * Class that implements redis value serializer with Kryo
 * {@see https://github.com/EsotericSoftware/kryo}
 */
@Slf4j
public class KryoRedisValueSerializer implements RedisSerializer<Object> {
    private static final int BUFFER_SIZE = 500;

    static {
        Log.set(Log.LEVEL_WARN);
        Log.setLogger(new Log.Logger() {
            @Override
            public void log(int level, String category, String message, Throwable ex) {
                // Level is set to WARN, so handling WARN and ERROR is enough here
                if (level == Log.LEVEL_WARN) {
                    log.warn("[{}]: {}", category, message, ex);
                } else if (level == Log.LEVEL_ERROR) {
                    log.error("[{}]: {}", category, message, ex);
                }
            }
        });
    }

    /**
     * Creates a thread safe instance of Kryo serialization framework.
     *
     * <ol>
     *
     *     <li> <strong>Instantiate strategy</strong> is set to
     *     default with `std` as a fallback. The `std` is set to
     *     avoid objects creation with the constructor as a lot of
     *     objects don't have no-arguments constructor. They don't
     *     implement {@link java.io.Serializable} neither. </li>
     *
     *     <li> <strong>Registration strategy</strong> is set
     *     to optional, we actually store A LOT of classes into
     *     caches and we can't risk an exception due to this. </li>
     *
     * </ol>
     */
    private static final Pool<KryoSerDes> KRYO_SER_DES_POOL = new Pool<>(true, false) {
        @Override
        protected KryoSerDes create() {
            Kryo kryo = new Kryo();

            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            kryo.setRegistrationRequired(false);
            kryo.setWarnUnregisteredClasses(true);

            return KryoSerDes.of(kryo, new Input(), new Output(BUFFER_SIZE, Util.maxArraySize));
        }
    };

    @Override
    @Nullable
    public byte[] serialize(@Nullable Object o) {
        if (o == null) {
            log.trace("Redis serialization of null value");
            return null;
        }

        log.trace("Redis cache serialization of {}", o.getClass());
        var kryoSerDes = KRYO_SER_DES_POOL.obtain();
        try {
            return kryoSerDes.serialize(o);
        } catch (Exception e) {
            throw new SerializationException("Serialization with Kryo failed", e);
        } finally {
            KRYO_SER_DES_POOL.free(kryoSerDes);
        }
    }

    @Override
    @Nullable
    public Object deserialize(@Nullable byte[] bytes) {
        if (bytes == null) {
            log.trace("Redis deserialization of null value");
            return null;
        }
        log.trace("Redis cache deserialization of {} bytes array", bytes.length);
        var kryoSerDes = KRYO_SER_DES_POOL.obtain();
        try {
            return kryoSerDes.deserialize(bytes);
        } catch (Exception e) {
            throw new SerializationException("Serialization with Kryo failed", e);
        } finally {
            KRYO_SER_DES_POOL.free(kryoSerDes);
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class KryoSerDes implements Pool.Poolable {
        private final Kryo kryo;
        private final Input input;
        private final Output output;

        public byte[] serialize(Object o) {
            kryo.writeClassAndObject(output, o);
            return output.toBytes();
        }

        public Object deserialize(byte[] bytes) {
            input.setBuffer(bytes);
            return kryo.readClassAndObject(input);
        }

        @Override
        public void reset() {
            kryo.reset();
            input.reset();
            output.reset();
        }
    }
}