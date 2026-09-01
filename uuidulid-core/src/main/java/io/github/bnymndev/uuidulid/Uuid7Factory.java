package io.github.bnymndev.uuidulid;

import java.time.Clock;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Generates version 7 UUIDs as defined by
 * <a href="https://www.rfc-editor.org/rfc/rfc9562#name-uuid-version-7">RFC 9562 &sect;5.7</a>.
 *
 * <p>A UUIDv7 is a time-ordered UUID: the first 48 bits are a Unix timestamp in milliseconds,
 * followed by the 4-bit version, 12 bits of randomness, the 2-bit variant and 62 further random
 * bits.
 *
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                          unix_ts_ms                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |        rand_a         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                         rand_b                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                             rand_b                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <p>Compared with a ULID a UUIDv7 carries the same ordering guarantees but only 74 random bits,
 * because six bits are spent on the version and variant fields. In exchange it is a valid
 * {@link UUID}, so it fits native {@code uuid} database columns and existing UUID tooling.
 *
 * <p>A factory is thread-safe and meant to be created once and shared. A
 * {@linkplain #monotonic() monotonic} factory keeps identifiers strictly increasing within a
 * millisecond by using the random fields as a counter, as described in RFC 9562 &sect;6.2.
 *
 * <pre>{@code
 * private static final Uuid7Factory IDS = Uuid7Factory.monotonic();
 *
 * UUID id = IDS.create();
 * }</pre>
 */
public final class Uuid7Factory implements Supplier<UUID> {

    /** Version 7 nibble, positioned at bits 12&ndash;15 of the most significant long. */
    private static final long VERSION_BITS = 0x7000L;

    /** RFC 9562 variant {@code 10x}, positioned at the top of the least significant long. */
    private static final long VARIANT_BITS = 0x8000_0000_0000_0000L;

    /** The 12-bit {@code rand_a} field. */
    private static final long RAND_A_MASK = 0x0FFFL;

    /** The 62-bit {@code rand_b} field. */
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private final Clock clock;
    private final LongSupplier random;
    private final boolean monotonic;

    private final ReentrantLock lock = new ReentrantLock();

    private long lastTimestamp = -1L;
    private long lastMsb;
    private long lastLsb;

    private Uuid7Factory(Clock clock, LongSupplier random, boolean monotonic) {
        this.clock = clock;
        this.random = random;
        this.monotonic = monotonic;
    }

    /**
     * Creates a factory that draws fresh randomness for every identifier.
     *
     * @return a new non-monotonic factory backed by {@link java.security.SecureRandom}
     */
    public static Uuid7Factory random() {
        return builder().monotonic(false).build();
    }

    /**
     * Creates a factory whose identifiers are strictly increasing.
     *
     * @return a new monotonic factory backed by {@link java.security.SecureRandom}
     */
    public static Uuid7Factory monotonic() {
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
     * @return a new version 7 UUID
     */
    public UUID create() {
        return monotonic ? createMonotonic() : createRandom();
    }

    /**
     * Equivalent to {@link #create()}.
     *
     * @return a new version 7 UUID
     */
    @Override
    public UUID get() {
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

    private UUID createRandom() {
        long timestamp = currentTimestamp();
        return new UUID(composeMsb(timestamp, random.getAsLong()), composeLsb(random.getAsLong()));
    }

    private UUID createMonotonic() {
        lock.lock();
        try {
            long now = currentTimestamp();
            if (now > lastTimestamp) {
                seed(now);
            } else {
                // Same millisecond, or the clock moved backwards: use the random fields as a
                // counter (RFC 9562 section 6.2) so ordering still holds. rand_b gives 2^62
                // identifiers per millisecond before rand_a has to absorb a carry.
                long randB = (lastLsb & RAND_B_MASK) + 1L;
                if (randB > RAND_B_MASK) {
                    long randA = (lastMsb & RAND_A_MASK) + 1L;
                    if (randA > RAND_A_MASK) {
                        seed(lastTimestamp + 1L);
                        return new UUID(lastMsb, lastLsb);
                    }
                    lastMsb = (lastMsb & ~RAND_A_MASK) | randA;
                    randB = 0L;
                }
                lastLsb = composeLsb(randB);
            }
            return new UUID(lastMsb, lastLsb);
        } finally {
            lock.unlock();
        }
    }

    private void seed(long timestamp) {
        checkTimestamp(timestamp);
        lastTimestamp = timestamp;
        lastMsb = composeMsb(timestamp, random.getAsLong());
        lastLsb = composeLsb(random.getAsLong());
    }

    private static long composeMsb(long timestamp, long randA) {
        return (timestamp << 16) | VERSION_BITS | (randA & RAND_A_MASK);
    }

    private static long composeLsb(long randB) {
        return VARIANT_BITS | (randB & RAND_B_MASK);
    }

    private long currentTimestamp() {
        long timestamp = clock.millis();
        checkTimestamp(timestamp);
        return timestamp;
    }

    private static void checkTimestamp(long timestamp) {
        if (timestamp < 0 || timestamp > Ulid.MAX_TIMESTAMP) {
            throw new IllegalStateException("Clock is outside the UUIDv7 timestamp range: " + timestamp);
        }
    }

    /** Builder for {@link Uuid7Factory}. */
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
         * @param random the generator to draw random bits from; must be safe for concurrent use
         *               if the factory is shared across threads
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
        public Uuid7Factory build() {
            return new Uuid7Factory(clock, random, monotonic);
        }
    }
}
