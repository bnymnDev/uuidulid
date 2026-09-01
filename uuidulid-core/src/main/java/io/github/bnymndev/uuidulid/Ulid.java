package io.github.bnymndev.uuidulid;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable ULID: a 128-bit, lexicographically sortable identifier.
 *
 * <p>The layout follows the <a href="https://github.com/ulid/spec">ULID specification</a>:
 *
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      32 bits of timestamp                     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |     16 bits of timestamp      |      16 bits of randomness    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                      64 bits of randomness                    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <p>The first 48 bits are a Unix timestamp in milliseconds, the remaining 80 bits are random.
 * The canonical text form is 26 Crockford Base32 characters, for example
 * {@code 01ARZ3NDEKTSV4RRFFQ69G5FAV}. Sorting those strings lexicographically yields the same
 * order as comparing the identifiers by creation time, which makes ULIDs a good fit for
 * database primary keys and keyset pagination.
 *
 * <p>Instances are immutable and thread-safe. Ordering is by unsigned 128-bit value and is
 * consistent with {@code equals} and with the natural ordering of {@link #toString()}.
 *
 * <p>Typical use:
 * <pre>{@code
 * Ulid id = Ulid.random();                       // one-off, non-monotonic
 * UlidFactory factory = UlidFactory.monotonic(); // hold as a singleton
 * Ulid next = factory.create();
 * }</pre>
 *
 * @see UlidFactory
 * @see Uuids
 */
public final class Ulid implements Comparable<Ulid>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Number of characters in the canonical text form. */
    public static final int LENGTH = 26;

    /** Number of bytes in the binary form. */
    public static final int BYTES = 16;

    /** Number of random bytes in a ULID. */
    public static final int RANDOMNESS_BYTES = 10;

    /** Largest representable timestamp, {@code 2^48 - 1} ms (10889-08-02T05:31:50.655Z). */
    public static final long MAX_TIMESTAMP = (1L << 48) - 1;

    /** The smallest possible ULID, {@code 00000000000000000000000000}. */
    public static final Ulid MIN = new Ulid(0L, 0L);

    /** The largest possible ULID, {@code 7ZZZZZZZZZZZZZZZZZZZZZZZZZ}. */
    public static final Ulid MAX = new Ulid(-1L, -1L);

    private final long msb;
    private final long lsb;

    private Ulid(long msb, long lsb) {
        this.msb = msb;
        this.lsb = lsb;
    }

    // ---------------------------------------------------------------- factories

    /**
     * Creates a ULID from its raw 128 bits.
     *
     * @param mostSignificantBits  the high 64 bits (48-bit timestamp plus 16 random bits)
     * @param leastSignificantBits the low 64 random bits
     * @return the ULID
     */
    public static Ulid of(long mostSignificantBits, long leastSignificantBits) {
        return new Ulid(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Creates a ULID from a timestamp and 10 bytes of randomness.
     *
     * @param timestamp  Unix time in milliseconds, in {@code [0, MAX_TIMESTAMP]}
     * @param randomness exactly {@value #RANDOMNESS_BYTES} bytes of randomness
     * @return the ULID
     * @throws IllegalArgumentException if the timestamp is out of range or the array has the
     *                                  wrong length
     * @throws NullPointerException     if {@code randomness} is {@code null}
     */
    public static Ulid of(long timestamp, byte[] randomness) {
        checkTimestamp(timestamp);
        if (randomness == null) {
            throw new NullPointerException("randomness must not be null");
        }
        if (randomness.length != RANDOMNESS_BYTES) {
            throw new IllegalArgumentException(
                    "randomness must be " + RANDOMNESS_BYTES + " bytes, was " + randomness.length);
        }
        long msb = (timestamp << 16)
                | ((randomness[0] & 0xFFL) << 8)
                | (randomness[1] & 0xFFL);
        long lsb = 0L;
        for (int i = 2; i < RANDOMNESS_BYTES; i++) {
            lsb = (lsb << 8) | (randomness[i] & 0xFFL);
        }
        return new Ulid(msb, lsb);
    }

    /**
     * Creates a ULID from an instant and 10 bytes of randomness.
     *
     * @param instant    the creation instant, truncated to milliseconds
     * @param randomness exactly {@value #RANDOMNESS_BYTES} bytes of randomness
     * @return the ULID
     * @throws IllegalArgumentException if the instant is out of range or the array has the
     *                                  wrong length
     */
    public static Ulid of(Instant instant, byte[] randomness) {
        return of(instant.toEpochMilli(), randomness);
    }

    /**
     * Generates a random ULID using a shared, thread-local {@link java.security.SecureRandom}.
     *
     * <p>Identifiers created by this method are <em>not</em> monotonic: two ULIDs created within
     * the same millisecond sort in an arbitrary order relative to each other. Use
     * {@link UlidFactory#monotonic()} when strict ordering within a millisecond matters.
     *
     * @return a new random ULID
     */
    public static Ulid random() {
        return DefaultFactoryHolder.RANDOM.create();
    }

    /**
     * Parses the canonical 26-character text form.
     *
     * <p>Parsing is case-insensitive and accepts the Crockford aliases {@code I}/{@code L} for
     * {@code 1} and {@code O} for {@code 0}.
     *
     * @param text the text to parse
     * @return the parsed ULID
     * @throws IllegalArgumentException if {@code text} is not a valid ULID
     */
    public static Ulid parse(CharSequence text) {
        long[] parts = new long[2];
        if (!CrockfordBase32.decode(text, parts)) {
            throw new IllegalArgumentException("Invalid ULID: \"" + text + "\"");
        }
        return new Ulid(parts[0], parts[1]);
    }

    /**
     * Parses the canonical text form without throwing on invalid input.
     *
     * @param text the text to parse, may be {@code null}
     * @return the parsed ULID, or {@link Optional#empty()} if {@code text} is not a valid ULID
     */
    public static Optional<Ulid> tryParse(CharSequence text) {
        long[] parts = new long[2];
        return CrockfordBase32.decode(text, parts)
                ? Optional.of(new Ulid(parts[0], parts[1]))
                : Optional.empty();
    }

    /**
     * Tests whether the given text is a valid ULID.
     *
     * @param text the text to test, may be {@code null}
     * @return {@code true} if {@code text} can be parsed by {@link #parse(CharSequence)}
     */
    public static boolean isValid(CharSequence text) {
        return CrockfordBase32.decode(text, new long[2]);
    }

    /**
     * Creates a ULID from its 16-byte big-endian binary form.
     *
     * @param bytes exactly {@value #BYTES} bytes
     * @return the ULID
     * @throws IllegalArgumentException if the array does not have exactly {@value #BYTES} bytes
     */
    public static Ulid fromBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        if (bytes.length != BYTES) {
            throw new IllegalArgumentException("bytes must be " + BYTES + " long, was " + bytes.length);
        }
        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFFL);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (bytes[i] & 0xFFL);
        }
        return new Ulid(msb, lsb);
    }

    /**
     * Reinterprets the 128 bits of a {@link UUID} as a ULID.
     *
     * <p>The bits are copied verbatim, so the conversion is lossless and round-trips through
     * {@link #toUuid()}. Applied to a UUIDv7 the result carries the same millisecond timestamp,
     * because both layouts start with a 48-bit big-endian Unix timestamp.
     *
     * @param uuid the UUID to convert
     * @return the ULID with the same 128 bits
     */
    public static Ulid fromUuid(UUID uuid) {
        return new Ulid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Returns the smallest ULID with the given timestamp, i.e. all randomness bits zero.
     *
     * <p>Together with {@link #max(long)} this brackets a time range, which is handy for
     * range queries on a ULID primary key:
     * {@code WHERE id >= :from AND id <= :to}.
     *
     * @param timestamp Unix time in milliseconds
     * @return the lower bound for that millisecond
     * @throws IllegalArgumentException if the timestamp is out of range
     */
    public static Ulid min(long timestamp) {
        checkTimestamp(timestamp);
        return new Ulid(timestamp << 16, 0L);
    }

    /**
     * Returns the smallest ULID with the given instant's timestamp.
     *
     * @param instant the instant, truncated to milliseconds
     * @return the lower bound for that millisecond
     * @throws IllegalArgumentException if the instant is out of range
     */
    public static Ulid min(Instant instant) {
        return min(instant.toEpochMilli());
    }

    /**
     * Returns the largest ULID with the given timestamp, i.e. all randomness bits set.
     *
     * @param timestamp Unix time in milliseconds
     * @return the upper bound for that millisecond
     * @throws IllegalArgumentException if the timestamp is out of range
     */
    public static Ulid max(long timestamp) {
        checkTimestamp(timestamp);
        return new Ulid((timestamp << 16) | 0xFFFFL, -1L);
    }

    /**
     * Returns the largest ULID with the given instant's timestamp.
     *
     * @param instant the instant, truncated to milliseconds
     * @return the upper bound for that millisecond
     * @throws IllegalArgumentException if the instant is out of range
     */
    public static Ulid max(Instant instant) {
        return max(instant.toEpochMilli());
    }

    // ---------------------------------------------------------------- accessors

    /**
     * Returns the embedded Unix timestamp in milliseconds.
     *
     * @return the timestamp, in {@code [0, MAX_TIMESTAMP]}
     */
    public long getTimestamp() {
        return msb >>> 16;
    }

    /**
     * Returns the embedded timestamp as an instant.
     *
     * @return the creation instant, at millisecond resolution
     */
    public Instant getInstant() {
        return Instant.ofEpochMilli(getTimestamp());
    }

    /**
     * Returns the high 64 bits: the 48-bit timestamp followed by 16 random bits.
     *
     * @return the most significant 64 bits
     */
    public long getMostSignificantBits() {
        return msb;
    }

    /**
     * Returns the low 64 bits, all of which are random.
     *
     * @return the least significant 64 bits
     */
    public long getLeastSignificantBits() {
        return lsb;
    }

    /**
     * Returns the 80-bit randomness component.
     *
     * @return a new array of {@value #RANDOMNESS_BYTES} bytes, big-endian
     */
    public byte[] getRandomness() {
        byte[] out = new byte[RANDOMNESS_BYTES];
        out[0] = (byte) (msb >>> 8);
        out[1] = (byte) msb;
        for (int i = 0; i < 8; i++) {
            out[2 + i] = (byte) (lsb >>> (56 - 8 * i));
        }
        return out;
    }

    /**
     * Returns the 16-byte big-endian binary form, suitable for a {@code BINARY(16)} column.
     *
     * @return a new array of {@value #BYTES} bytes
     */
    public byte[] toBytes() {
        byte[] out = new byte[BYTES];
        for (int i = 0; i < 8; i++) {
            out[i] = (byte) (msb >>> (56 - 8 * i));
            out[8 + i] = (byte) (lsb >>> (56 - 8 * i));
        }
        return out;
    }

    /**
     * Reinterprets the 128 bits of this ULID as a {@link UUID}.
     *
     * <p>The bits are copied verbatim, so the conversion round-trips through
     * {@link #fromUuid(UUID)}. Note that the result is <em>not</em> an RFC 9562 UUID: the
     * version and variant fields hold random ULID bits rather than the values those fields are
     * required to have. It is well suited to storing a ULID in a native {@code uuid} column,
     * not to being handed out as a UUID.
     *
     * @return a UUID with the same 128 bits
     */
    public UUID toUuid() {
        return new UUID(msb, lsb);
    }

    /**
     * Returns the ULID that follows this one, treating the value as an unsigned 128-bit integer.
     *
     * <p>Useful as an exclusive bound in keyset pagination.
     *
     * @return the next ULID
     * @throws ArithmeticException if this ULID is {@link #MAX}
     */
    public Ulid increment() {
        long nextLsb = lsb + 1;
        if (nextLsb != 0) {
            return new Ulid(msb, nextLsb);
        }
        if (msb == -1L) {
            throw new ArithmeticException("ULID overflow: cannot increment " + MAX);
        }
        return new Ulid(msb + 1, 0L);
    }

    // ---------------------------------------------------------------- object contract

    /**
     * Returns the canonical 26-character uppercase Crockford Base32 form.
     *
     * @return the text form, for example {@code 01ARZ3NDEKTSV4RRFFQ69G5FAV}
     */
    @Override
    public String toString() {
        return CrockfordBase32.encode(msb, lsb);
    }

    /**
     * Compares two ULIDs as unsigned 128-bit integers.
     *
     * <p>The ordering matches the lexicographic ordering of {@link #toString()} and, for
     * identifiers created in different milliseconds, the order in which they were created.
     *
     * @param other the ULID to compare against
     * @return a negative value, zero or a positive value as this ULID sorts before, equal to,
     *         or after {@code other}
     */
    @Override
    public int compareTo(Ulid other) {
        int cmp = Long.compareUnsigned(this.msb, other.msb);
        return cmp != 0 ? cmp : Long.compareUnsigned(this.lsb, other.lsb);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Ulid)) {
            return false;
        }
        Ulid other = (Ulid) obj;
        return this.msb == other.msb && this.lsb == other.lsb;
    }

    @Override
    public int hashCode() {
        long hilo = msb ^ lsb;
        return (int) (hilo >> 32) ^ (int) hilo;
    }

    // ---------------------------------------------------------------- internals

    private static void checkTimestamp(long timestamp) {
        if (timestamp < 0 || timestamp > MAX_TIMESTAMP) {
            throw new IllegalArgumentException(
                    "timestamp must be in [0, " + MAX_TIMESTAMP + "], was " + timestamp);
        }
    }

    /** Lazily initialised default factory, so that {@code Ulid} itself has no static state cost. */
    private static final class DefaultFactoryHolder {
        static final UlidFactory RANDOM = UlidFactory.random();

        private DefaultFactoryHolder() {
        }
    }
}
