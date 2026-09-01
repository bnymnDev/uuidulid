package io.github.bnymndev.uuidulid;

import java.time.Clock;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Generates {@link Ulid} values.
 *
 * <p>A factory is thread-safe and meant to be created once and shared &mdash; as a static final
 * field, or as a Spring bean when using {@code uuidulid-spring-boot-starter}. It implements
 * {@link Supplier}, so it can be injected wherever a {@code Supplier<Ulid>} is expected.
 *
 * <h2>Monotonic vs. random</h2>
 * <p>A {@linkplain #monotonic() monotonic} factory guarantees that every identifier it returns
 * sorts strictly after the previous one, even within the same millisecond and even if the system
 * clock jumps backwards. It does so as the ULID specification prescribes: within a millisecond
 * the 80-bit randomness component is incremented by one. That makes consecutive identifiers from
 * the same millisecond guessable from one another, so prefer a {@linkplain #random() random}
 * factory when identifiers are handed out publicly and must not be enumerable.
 *
 * <p>A {@linkplain #random() random} factory draws fresh randomness for every identifier.
 * Ordering across milliseconds still holds; only ties within one millisecond are arbitrary.
 *
 * <pre>{@code
 * private static final UlidFactory IDS = UlidFactory.monotonic();
 *
 * Ulid id = IDS.create();
 * }</pre>
 */
public final class UlidFactory implements Supplier<Ulid> {

    private final Clock clock;
    private final LongSupplier random;
    private final boolean monotonic;

    /** Guards the monotonic state below; unused by random factories. */
    private final ReentrantLock lock = new ReentrantLock();

    private long lastTimestamp = -1L;
    private long lastMsb;
    private long lastLsb;

    private UlidFactory(Clock clock, LongSupplier random, boolean monotonic) {
        this.clock = clock;
        this.random = random;
        this.monotonic = monotonic;
    }

    /**
     * Creates a factory that draws fresh randomness for every identifier.
     *
     * @return a new non-monotonic factory backed by {@link java.security.SecureRandom}
     */
    public static UlidFactory random() {
        return builder().monotonic(false).build();
    }

    /**
     * Creates a factory whose identifiers are strictly increasing.
     *
     * @return a new monotonic factory backed by {@link java.security.SecureRandom}
     */
    public static UlidFactory monotonic() {
        return builder().monotonic(true).build();
    }

    /**
     * Returns a builder for a factory with a custom clock or random generator.
     *
     * @return a new builder, monotonic by default
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates the next identifier.
     *
     * @return a new ULID
     */
    public Ulid create() {
        return monotonic ? createMonotonic() : createRandom();
    }

    /**
     * Equivalent to {@link #create()}.
     *
     * @return a new ULID
     */
    @Override
    public Ulid get() {
        return create();
    }

    /**
     * Tells whether this factory guarantees strictly increasing identifiers.
     *
     * @return {@code true} for a monotonic factory
     */
    public boolean isMonotonic() {
        return monotonic;
    }

    private Ulid createRandom() {
        long timestamp = currentTimestamp();
        long msb = (timestamp << 16) | (random.getAsLong() & 0xFFFFL);
        return Ulid.of(msb, random.getAsLong());
    }

    private Ulid createMonotonic() {
        lock.lock();
        try {
            long now = currentTimestamp();
            if (now > lastTimestamp) {
                lastTimestamp = now;
                lastMsb = (now << 16) | (random.getAsLong() & 0xFFFFL);
                lastLsb = random.getAsLong();
            } else {
                // Same millisecond, or the clock moved backwards. Increment the 128-bit value so
                // the result still sorts after the previous one. A carry out of the 80 random
                // bits rolls into the timestamp, which is the behaviour we want: the next
                // identifier simply belongs to the following millisecond.
                if (++lastLsb == 0L) {
                    if (++lastMsb == 0L) {
                        throw new ArithmeticException("ULID space exhausted");
                    }
                    lastTimestamp = lastMsb >>> 16;
                }
            }
            return Ulid.of(lastMsb, lastLsb);
        } finally {
            lock.unlock();
        }
    }

    private long currentTimestamp() {
        long timestamp = clock.millis();
        if (timestamp < 0 || timestamp > Ulid.MAX_TIMESTAMP) {
            throw new IllegalStateException("Clock is outside the ULID timestamp range: " + timestamp);
        }
        return timestamp;
    }

    /** Builder for {@link UlidFactory}. */
    public static final class Builder {

        private Clock clock = Clock.systemUTC();
        private LongSupplier random = RandomSources.secure();
        private boolean monotonic = true;

        private Builder() {
        }

        /**
         * Sets the clock used to read the current time. Defaults to {@link Clock#systemUTC()}.
         *
         * @param clock the clock, typically a fixed clock in tests
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets the source of randomness. Defaults to a per-thread
         * {@link java.security.SecureRandom}.
         *
         * <p>Supplying a faster non-cryptographic generator (for example
         * {@link java.util.concurrent.ThreadLocalRandom}) makes identifiers predictable and
         * should be limited to internal identifiers that are never used as capabilities. The
         * generator must be safe for concurrent use if the factory is shared across threads.
         *
         * @param random the generator to draw random bits from
         * @return this builder
         */
        public Builder random(Random random) {
            Objects.requireNonNull(random, "random");
            this.random = RandomSources.of(random);
            return this;
        }

        /**
         * Sets an arbitrary source of random 64-bit values, for generators that do not extend
         * {@link Random} (for example the {@code java.util.random.RandomGenerator} implementations
         * of Java 17+, passed as {@code generator::nextLong}).
         *
         * @param source the supplier of random longs; must be safe for concurrent use if the
         *               factory is shared across threads
         * @return this builder
         */
        public Builder randomSource(LongSupplier source) {
            this.random = Objects.requireNonNull(source, "source");
            return this;
        }

        /**
         * Enables or disables the monotonic guarantee. Defaults to {@code true}.
         *
         * @param monotonic whether identifiers must be strictly increasing
         * @return this builder
         */
        public Builder monotonic(boolean monotonic) {
            this.monotonic = monotonic;
            return this;
        }

        /**
         * Builds the factory.
         *
         * @return a new factory
         */
        public UlidFactory build() {
            return new UlidFactory(clock, random, monotonic);
        }
    }
}
