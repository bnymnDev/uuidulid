package io.github.bnymndev.uuidulid;

import java.security.SecureRandom;
import java.util.function.LongSupplier;
import java.util.Random;

/** Internal helpers for obtaining random bits for identifier generation. */
final class RandomSources {

    /**
     * One {@link SecureRandom} per thread. {@code SecureRandom} is thread-safe but synchronises
     * internally, which turns into a contention point when many request threads generate
     * identifiers at once. A per-thread instance avoids that lock entirely.
     */
    private static final ThreadLocal<SecureRandom> THREAD_LOCAL_SECURE = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    private RandomSources() {
    }

    /**
     * Returns the default cryptographically strong source of random bits.
     *
     * @return a thread-safe supplier of random longs
     */
    static LongSupplier secure() {
        return new LongSupplier() {
            @Override
            public long getAsLong() {
                return THREAD_LOCAL_SECURE.get().nextLong();
            }
        };
    }

    /**
     * Adapts a caller-supplied generator.
     *
     * @param generator the generator to use; the caller is responsible for its thread-safety
     * @return a supplier of random longs backed by {@code generator}
     */
    static LongSupplier of(final Random random) {
        return new LongSupplier() {
            @Override
            public long getAsLong() {
                return random.nextLong();
            }
        };
    }
}
