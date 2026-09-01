package io.github.bnymndev.uuidulid;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Static helpers for working with {@link UUID} values: generating version 4 and version 7 UUIDs,
 * reading the embedded timestamp of time-based versions, lenient parsing, binary conversion and
 * correct unsigned ordering.
 *
 * <p>For high-volume or strictly ordered generation use {@link Uuid7Factory} instead of
 * {@link #v7()}; a shared factory can keep identifiers monotonic within a millisecond.
 */
public final class Uuids {

    /** The Nil UUID, {@code 00000000-0000-0000-0000-000000000000} (RFC 9562 &sect;5.9). */
    public static final UUID NIL = new UUID(0L, 0L);

    /** The Max UUID, {@code ffffffff-ffff-ffff-ffff-ffffffffffff} (RFC 9562 &sect;5.10). */
    public static final UUID MAX = new UUID(-1L, -1L);

    /** 100-nanosecond intervals between 1582-10-15 (the UUID epoch) and 1970-01-01. */
    private static final long GREGORIAN_OFFSET_TICKS = 0x01B21DD213814000L;

    /** 100-nanosecond intervals per millisecond. */
    private static final long TICKS_PER_MILLI = 10_000L;

    private Uuids() {
    }

    // ---------------------------------------------------------------- generation

    /**
     * Generates a random version 4 UUID using a per-thread
     * {@link java.security.SecureRandom}.
     *
     * @return a new version 4 UUID
     */
    public static UUID v4() {
        long msb = (Holder.RANDOM.getAsLong() & 0xFFFF_FFFF_FFFF_0FFFL) | 0x0000_0000_0000_4000L;
        long lsb = (Holder.RANDOM.getAsLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    /**
     * Generates a version 7 UUID using a shared non-monotonic factory.
     *
     * <p>Identifiers created within the same millisecond sort in an arbitrary order relative to
     * each other. Use {@link Uuid7Factory#monotonic()} when that matters.
     *
     * @return a new version 7 UUID
     */
    public static UUID v7() {
        return Holder.V7.create();
    }

    // ---------------------------------------------------------------- inspection

    /**
     * Returns the Unix timestamp in milliseconds embedded in a time-based UUID.
     *
     * @param uuid a UUID of version 1, 6 or 7
     * @return the timestamp in milliseconds since the Unix epoch
     * @throws IllegalArgumentException if the UUID does not carry a timestamp
     */
    public static long timestamp(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        switch (uuid.version()) {
            case 1:
                return fromGregorianTicks(
                        ((msb & 0x0000_0000_0000_0FFFL) << 48)
                                | ((msb & 0x0000_0000_FFFF_0000L) << 16)
                                | (msb >>> 32));
            case 6:
                return fromGregorianTicks(
                        ((msb >>> 32) << 28)
                                | ((msb & 0x0000_0000_FFFF_0000L) >>> 4)
                                | (msb & 0x0000_0000_0000_0FFFL));
            case 7:
                return msb >>> 16;
            default:
                throw new IllegalArgumentException(
                        "UUID version " + uuid.version() + " does not carry a timestamp: " + uuid);
        }
    }

    /**
     * Returns the instant embedded in a time-based UUID.
     *
     * @param uuid a UUID of version 1, 6 or 7
     * @return the creation instant, at millisecond resolution
     * @throws IllegalArgumentException if the UUID does not carry a timestamp
     */
    public static Instant instant(UUID uuid) {
        return Instant.ofEpochMilli(timestamp(uuid));
    }

    /**
     * Tests whether a UUID is a version 7 UUID with the RFC 9562 variant.
     *
     * @param uuid the UUID to test, may be {@code null}
     * @return {@code true} if the version field is 7 and the variant field is {@code 10x}
     */
    public static boolean isV7(UUID uuid) {
        return uuid != null && uuid.version() == 7 && uuid.variant() == 2;
    }

    // ---------------------------------------------------------------- conversion

    /**
     * Reinterprets the 128 bits of a UUID as a {@link Ulid}.
     *
     * @param uuid the UUID to convert
     * @return the ULID with the same 128 bits
     * @see Ulid#fromUuid(UUID)
     */
    public static Ulid toUlid(UUID uuid) {
        return Ulid.fromUuid(uuid);
    }

    /**
     * Reinterprets the 128 bits of a ULID as a UUID.
     *
     * @param ulid the ULID to convert
     * @return the UUID with the same 128 bits
     * @see Ulid#toUuid()
     */
    public static UUID fromUlid(Ulid ulid) {
        return ulid.toUuid();
    }

    /**
     * Returns the 16-byte big-endian binary form, suitable for a {@code BINARY(16)} column.
     *
     * @param uuid the UUID to convert
     * @return a new array of 16 bytes
     */
    public static byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] out = new byte[16];
        for (int i = 0; i < 8; i++) {
            out[i] = (byte) (msb >>> (56 - 8 * i));
            out[8 + i] = (byte) (lsb >>> (56 - 8 * i));
        }
        return out;
    }

    /**
     * Reads a UUID from its 16-byte big-endian binary form.
     *
     * @param bytes exactly 16 bytes
     * @return the UUID
     * @throws IllegalArgumentException if the array does not have exactly 16 bytes
     */
    public static UUID fromBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        if (bytes.length != 16) {
            throw new IllegalArgumentException("bytes must be 16 long, was " + bytes.length);
        }
        long msb = 0L;
        long lsb = 0L;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[i] & 0xFFL);
            lsb = (lsb << 8) | (bytes[8 + i] & 0xFFL);
        }
        return new UUID(msb, lsb);
    }

    // ---------------------------------------------------------------- parsing

    /**
     * Parses a UUID, accepting the canonical dashed form as well as the bare 32-digit form, a
     * {@code urn:uuid:} prefix and enclosing braces.
     *
     * <p>{@link UUID#fromString(String)} in the JDK is lenient in a different, surprising way: it
     * accepts groups of the wrong length such as {@code 1-1-1-1-1}. This method requires exactly
     * 32 hexadecimal digits.
     *
     * @param text the text to parse
     * @return the parsed UUID
     * @throws IllegalArgumentException if {@code text} is not a valid UUID
     */
    public static UUID parse(CharSequence text) {
        UUID parsed = parseOrNull(text);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid UUID: \"" + text + "\"");
        }
        return parsed;
    }

    /**
     * Parses a UUID without throwing on invalid input.
     *
     * @param text the text to parse, may be {@code null}
     * @return the parsed UUID, or {@link Optional#empty()} if {@code text} is not a valid UUID
     */
    public static Optional<UUID> tryParse(CharSequence text) {
        return Optional.ofNullable(parseOrNull(text));
    }

    /**
     * Tests whether the given text is a valid UUID.
     *
     * @param text the text to test, may be {@code null}
     * @return {@code true} if {@code text} can be parsed by {@link #parse(CharSequence)}
     */
    public static boolean isValid(CharSequence text) {
        return parseOrNull(text) != null;
    }

    // ---------------------------------------------------------------- ordering

    /**
     * Compares two UUIDs as unsigned 128-bit integers.
     *
     * <p>{@link UUID#compareTo(UUID)} compares the two halves as <em>signed</em> longs, which
     * does not match the ordering of the textual form or of a {@code BINARY(16)} column. This
     * method does.
     *
     * @param a the first UUID
     * @param b the second UUID
     * @return a negative value, zero or a positive value as {@code a} sorts before, equal to, or
     *         after {@code b}
     */
    public static int compareUnsigned(UUID a, UUID b) {
        int cmp = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return cmp != 0 ? cmp : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    /**
     * Returns a comparator ordering UUIDs as unsigned 128-bit integers.
     *
     * @return an unsigned comparator, consistent with the ordering of the textual form
     */
    public static Comparator<UUID> unsignedComparator() {
        return UnsignedComparator.INSTANCE;
    }

    // ---------------------------------------------------------------- internals

    private static UUID parseOrNull(CharSequence text) {
        if (text == null) {
            return null;
        }
        int start = 0;
        int end = text.length();
        if (end >= 9 && equalsIgnoreCaseAt(text, 0, "urn:uuid:")) {
            start = 9;
        } else if (end >= 2 && text.charAt(0) == '{' && text.charAt(end - 1) == '}') {
            start = 1;
            end--;
        }

        long msb = 0L;
        long lsb = 0L;
        int digits = 0;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '-') {
                continue;
            }
            int nibble = Character.digit(c, 16);
            if (nibble < 0 || ++digits > 32) {
                return null;
            }
            if (digits <= 16) {
                msb = (msb << 4) | nibble;
            } else {
                lsb = (lsb << 4) | nibble;
            }
        }
        return digits == 32 ? new UUID(msb, lsb) : null;
    }

    private static boolean equalsIgnoreCaseAt(CharSequence text, int offset, String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            if (Character.toLowerCase(text.charAt(offset + i)) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long fromGregorianTicks(long ticks) {
        return (ticks - GREGORIAN_OFFSET_TICKS) / TICKS_PER_MILLI;
    }

    /** Stateless, so a single shared instance suffices. */
    private static final class UnsignedComparator implements Comparator<UUID>, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        static final UnsignedComparator INSTANCE = new UnsignedComparator();

        @Override
        public int compare(UUID a, UUID b) {
            return compareUnsigned(a, b);
        }
    }

    /** Lazily initialised shared state. */
    private static final class Holder {
        static final java.util.function.LongSupplier RANDOM = RandomSources.secure();
        static final Uuid7Factory V7 = Uuid7Factory.random();

        private Holder() {
        }
    }
}
